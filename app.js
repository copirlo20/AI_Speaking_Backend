/* ========================================
   AI Speaking Test - Application Logic
   ======================================== */

// ==================== CONFIGURATION ====================
const API_BASE = 'http://localhost:8080';

// ==================== STATE ====================
let allExams = [];
let filteredExams = [];
let currentPage = 0;
let pageSize = 8;
let totalPages = 0;

let selectedExam = null;
let testSession = null;
let examQuestions = [];
let currentQuestionIndex = 0;
let answersMap = {}; // questionId -> answer response

// Recording
let mediaRecorder = null;
let audioChunks = [];
let recordingStream = null;
let recordingInterval = null;
let recordingSeconds = 0;
let currentAudioBlob = null;

// ==================== SCREEN NAVIGATION ====================
function goToScreen(screenId) {
    document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
    document.getElementById(screenId).classList.add('active');
    window.scrollTo(0, 0);
}

// ==================== TOAST NOTIFICATIONS ====================
function showToast(message, type) {
    type = type || 'info';
    var container = document.getElementById('toast-container');
    var toast = document.createElement('div');
    toast.className = 'toast ' + type;

    var icons = {
        success: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>',
        error: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>',
        warning: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>',
        info: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>'
    };

    toast.innerHTML = (icons[type] || icons.info) + '<span>' + message + '</span>';
    container.appendChild(toast);

    setTimeout(function() {
        toast.style.animation = 'slideOutRight 0.3s ease forwards';
        setTimeout(function() { toast.remove(); }, 300);
    }, 4000);
}

// ==================== API HELPERS ====================
function apiGet(path) {
    return fetch(API_BASE + path)
        .then(function(res) {
            if (!res.ok) throw new Error('API Error: ' + res.status);
            return res.json();
        });
}

function apiPost(path, body) {
    return fetch(API_BASE + path, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    }).then(function(res) {
        if (!res.ok) return res.text().then(function(t) { throw new Error(t || 'API Error: ' + res.status); });
        return res.json();
    });
}

function apiPostMultipart(path, formData) {
    return fetch(API_BASE + path, {
        method: 'POST',
        body: formData
    }).then(function(res) {
        if (!res.ok) return res.text().then(function(t) { throw new Error(t || 'API Error: ' + res.status); });
        return res.json();
    });
}

// ==================== EXAM LIST (Screen 1) ====================
function loadExams(page) {
    if (page === undefined) page = 0;
    currentPage = page;

    var listEl = document.getElementById('exam-list');
    listEl.innerHTML = '<div class="loading-spinner"><div class="spinner"></div><p>Đang tải danh sách đề thi...</p></div>';

    apiGet('/exams/search?status=ACTIVE&page=' + page + '&size=' + pageSize + '&sort=createdAt,desc')
        .then(function(data) {
            allExams = data.content || [];
            totalPages = data.totalPages || 1;
            filteredExams = allExams;
            renderExamList(filteredExams);
            renderPagination();
        })
        .catch(function(err) {
            console.error(err);
            listEl.innerHTML = '<div class="empty-state"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg><p>Không thể tải danh sách đề thi.<br>Vui lòng kiểm tra kết nối và thử lại.</p></div>';
        });
}

function renderExamList(exams) {
    var listEl = document.getElementById('exam-list');

    if (!exams || exams.length === 0) {
        listEl.innerHTML = '<div class="empty-state"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg><p>Chưa có đề thi nào.</p></div>';
        return;
    }

    var html = '';
    for (var i = 0; i < exams.length; i++) {
        var exam = exams[i];
        var delay = i * 0.05;
        html += '<div class="exam-card" onclick="selectExam(' + exam.id + ')" style="animation-delay: ' + delay + 's">' +
            '<div class="exam-icon">' + (i + 1 + currentPage * pageSize) + '</div>' +
            '<div class="exam-info">' +
                '<div class="exam-name">' + escapeHtml(exam.name) + '</div>' +
                '<div class="exam-desc">' + escapeHtml(exam.description || 'Không có mô tả') + '</div>' +
                '<div class="exam-meta">' +
                    '<span class="meta-tag"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/><rect x="9" y="3" width="6" height="4" rx="1"/></svg>' + (exam.totalQuestions || 0) + ' câu hỏi</span>' +
                    (exam.durationMinutes ? '<span class="meta-tag"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>' + exam.durationMinutes + ' phút</span>' : '') +
                '</div>' +
            '</div>' +
            '<div class="exam-arrow"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg></div>' +
        '</div>';
    }
    listEl.innerHTML = html;
}

function renderPagination() {
    var pag = document.getElementById('exam-pagination');
    if (totalPages <= 1) { pag.innerHTML = ''; return; }

    var html = '';
    for (var i = 0; i < totalPages; i++) {
        html += '<button class="' + (i === currentPage ? 'active' : '') + '" onclick="loadExams(' + i + ')">' + (i + 1) + '</button>';
    }
    pag.innerHTML = html;
}

function filterExams() {
    var q = document.getElementById('exam-search').value.toLowerCase().trim();
    if (!q) {
        filteredExams = allExams;
    } else {
        filteredExams = allExams.filter(function(exam) {
            return (exam.name && exam.name.toLowerCase().indexOf(q) !== -1) ||
                   (exam.description && exam.description.toLowerCase().indexOf(q) !== -1);
        });
    }
    renderExamList(filteredExams);
}

function selectExam(examId) {
    var exam = allExams.find(function(e) { return e.id === examId; });
    if (!exam) return;
    selectedExam = exam;

    // Render selected exam info
    var info = document.getElementById('selected-exam-info');
    info.innerHTML = '<div class="exam-label">Đề thi đã chọn</div>' +
        '<div class="exam-name-display">' + escapeHtml(exam.name) + '</div>';

    goToScreen('screen-register');
}

// ==================== REGISTRATION & START (Screen 2) ====================
function startExam(event) {
    event.preventDefault();

    var name = document.getElementById('student-name').value.trim();
    var org = document.getElementById('student-org').value.trim();

    if (!name) {
        showToast('Vui lòng nhập họ và tên', 'warning');
        return;
    }

    var submitBtn = document.querySelector('.btn-start');
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<div class="spinner" style="width:20px;height:20px;border-width:2px;margin:0"></div> Đang tạo phiên thi...';

    var body = {
        examId: selectedExam.id,
        studentName: name
    };
    if (org) body.studentOrganization = org;

    apiPost('/test-sessions', body)
        .then(function(session) {
            testSession = session;
            return apiGet('/exams/' + selectedExam.id + '/questions');
        })
        .then(function(questions) {
            examQuestions = questions || [];
            if (examQuestions.length === 0) {
                showToast('Đề thi chưa có câu hỏi nào!', 'error');
                return;
            }

            // Load existing answers if any (only ones actually submitted)
            return apiGet('/test-sessions/' + testSession.id + '/answers')
                .then(function(answers) {
                    answersMap = {};
                    if (answers && answers.length) {
                        for (var i = 0; i < answers.length; i++) {
                            // Only store answers that were actually submitted (have answeredAt)
                            if (answers[i].answeredAt) {
                                answersMap[answers[i].questionId] = answers[i];
                            }
                        }
                    }
                    initExamUI();
                    goToScreen('screen-exam');
                });
        })
        .catch(function(err) {
            console.error(err);
            showToast('Không thể bắt đầu bài thi. Vui lòng thử lại.', 'error');
        })
        .finally(function() {
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="5 3 19 12 5 21 5 3"/></svg> Bắt đầu thi';
        });
}

// ==================== EXAM UI (Screen 3) ====================
function initExamUI() {
    document.getElementById('exam-title-bar').textContent = selectedExam.name;
    document.getElementById('student-name-bar').textContent = document.getElementById('student-name').value.trim();

    currentQuestionIndex = 0;
    renderQuestionNav();
    renderQuestion();
    updateProgress();
}

function renderQuestionNav() {
    var nav = document.getElementById('question-nav-pills');
    var html = '';
    for (var i = 0; i < examQuestions.length; i++) {
        var q = examQuestions[i];
        var isActive = i === currentQuestionIndex;
        var a = answersMap[q.questionId];
        var isAnswered = a !== undefined && a.answeredAt;
        var cls = 'q-pill';
        if (isActive) cls += ' active';
        if (isAnswered) cls += ' answered';
        html += '<button class="' + cls + '" onclick="goToQuestion(' + i + ')">' + (i + 1) + '</button>';
    }
    nav.innerHTML = html;
}

function renderQuestion() {
    var q = examQuestions[currentQuestionIndex];
    if (!q) return;

    document.getElementById('question-number').textContent = 'Câu ' + (currentQuestionIndex + 1);
    document.getElementById('question-content').textContent = q.questionContent;

    // Fetch question detail for level
    apiGet('/questions/' + q.questionId)
        .then(function(detail) {
            var levelEl = document.getElementById('question-level');
            if (detail && detail.level) {
                levelEl.textContent = detail.level === 'EASY' ? 'Dễ' : 'Khó';
                levelEl.className = 'level-badge ' + detail.level.toLowerCase();
            } else {
                levelEl.textContent = '';
                levelEl.className = 'level-badge';
            }
        })
        .catch(function() {
            // silently ignore
        });

    // Check if already answered (only if actually submitted - has answeredAt)
    var answer = answersMap[q.questionId];
    if (answer && answer.answeredAt && (answer.processingStatus === 'COMPLETED' || answer.processingStatus === 'FAILED')) {
        showFeedback(answer);
    } else if (answer && answer.answeredAt && (answer.processingStatus === 'TRANSCRIBING' || answer.processingStatus === 'SCORING')) {
        // Still processing - show submitting UI and resume polling
        document.getElementById('recording-section').classList.add('hidden');
        document.getElementById('submitting-section').classList.remove('hidden');
        document.getElementById('feedback-section').classList.add('hidden');
        pollAnswerResult(answer.id, q.questionId);
    } else {
        showRecordingUI();
    }

    // Update nav
    renderQuestionNav();
    updateNavButtons();
}

function updateProgress() {
    var answered = Object.keys(answersMap).length;
    var total = examQuestions.length;
    document.getElementById('progress-text').textContent = answered + ' / ' + total;
    var pct = total > 0 ? (answered / total * 100) : 0;
    document.getElementById('progress-fill').style.width = pct + '%';
}

function updateNavButtons() {
    document.getElementById('btn-prev').disabled = currentQuestionIndex === 0;
    document.getElementById('btn-next').disabled = currentQuestionIndex === examQuestions.length - 1;
}

function goToQuestion(index) {
    if (index < 0 || index >= examQuestions.length) return;

    // Stop recording if active
    if (mediaRecorder && mediaRecorder.state === 'recording') {
        stopRecording();
    }

    currentQuestionIndex = index;
    renderQuestion();
}

function showRecordingUI() {
    document.getElementById('recording-section').classList.remove('hidden');
    document.getElementById('submitting-section').classList.add('hidden');
    document.getElementById('feedback-section').classList.add('hidden');

    // Reset recording state
    document.getElementById('mic-button').classList.remove('recording');
    document.getElementById('mic-icon').classList.remove('hidden');
    document.getElementById('stop-icon').classList.add('hidden');
    document.getElementById('recording-wave').classList.add('hidden');
    document.getElementById('recording-status').textContent = 'Nhấn vào micro để bắt đầu ghi âm';
    document.getElementById('recording-timer').classList.add('hidden');
    document.getElementById('audio-playback').classList.add('hidden');
    currentAudioBlob = null;
}

function showFeedback(answer) {
    document.getElementById('recording-section').classList.add('hidden');
    document.getElementById('submitting-section').classList.add('hidden');
    document.getElementById('feedback-section').classList.remove('hidden');

    var score = parseFloat(answer.score) || 0;
    document.getElementById('feedback-score').textContent = score.toFixed(1);

    // Animate score ring
    var circle = document.getElementById('score-circle');
    var circumference = 2 * Math.PI * 52; // r=52
    var offset = circumference - (score / 10) * circumference;
    circle.style.strokeDashoffset = offset;

    // Color based on score
    var color = score >= 7 ? '#48bb78' : score >= 5 ? '#ed8936' : '#e53e3e';
    circle.style.stroke = color;
    document.getElementById('feedback-score').style.color = color;

    document.getElementById('feedback-transcribed').textContent = answer.transcribedText || '(Không nhận dạng được nội dung)';
    document.getElementById('feedback-comment').textContent = answer.feedback || 'Không có nhận xét.';
}

// ==================== AUDIO RECORDING ====================
function toggleRecording() {
    if (mediaRecorder && mediaRecorder.state === 'recording') {
        stopRecording();
    } else {
        startRecording();
    }
}

function startRecording() {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        showToast('Trình duyệt không hỗ trợ ghi âm. Vui lòng sử dụng Chrome hoặc Firefox.', 'error');
        return;
    }

    navigator.mediaDevices.getUserMedia({ audio: true })
        .then(function(stream) {
            recordingStream = stream;
            audioChunks = [];

            // Use a supported mime type
            var options = {};
            if (MediaRecorder.isTypeSupported('audio/webm;codecs=opus')) {
                options.mimeType = 'audio/webm;codecs=opus';
            } else if (MediaRecorder.isTypeSupported('audio/webm')) {
                options.mimeType = 'audio/webm';
            } else if (MediaRecorder.isTypeSupported('audio/ogg;codecs=opus')) {
                options.mimeType = 'audio/ogg;codecs=opus';
            }

            mediaRecorder = new MediaRecorder(stream, options);

            mediaRecorder.ondataavailable = function(event) {
                if (event.data.size > 0) {
                    audioChunks.push(event.data);
                }
            };

            mediaRecorder.onstop = function() {
                var blob = new Blob(audioChunks, { type: mediaRecorder.mimeType });
                // Convert to WAV
                convertToWav(blob).then(function(wavBlob) {
                    currentAudioBlob = wavBlob;
                    var url = URL.createObjectURL(wavBlob);
                    var player = document.getElementById('audio-player');
                    player.src = url;
                    document.getElementById('audio-playback').classList.remove('hidden');
                    document.getElementById('recording-status').textContent = 'Ghi âm hoàn tất. Nghe lại hoặc nộp câu trả lời.';
                }).catch(function() {
                    // Fallback: use original blob
                    currentAudioBlob = blob;
                    var url = URL.createObjectURL(blob);
                    var player = document.getElementById('audio-player');
                    player.src = url;
                    document.getElementById('audio-playback').classList.remove('hidden');
                    document.getElementById('recording-status').textContent = 'Ghi âm hoàn tất. Nghe lại hoặc nộp câu trả lời.';
                });

                // Stop tracks
                if (recordingStream) {
                    recordingStream.getTracks().forEach(function(t) { t.stop(); });
                }
            };

            mediaRecorder.start(250); // collect data every 250ms

            // UI Updates
            document.getElementById('mic-button').classList.add('recording');
            document.getElementById('mic-icon').classList.add('hidden');
            document.getElementById('stop-icon').classList.remove('hidden');
            document.getElementById('recording-wave').classList.remove('hidden');
            document.getElementById('recording-status').textContent = 'Đang ghi âm... Nhấn để dừng';
            document.getElementById('audio-playback').classList.add('hidden');

            // Timer
            recordingSeconds = 0;
            document.getElementById('recording-timer').classList.remove('hidden');
            updateRecordingTimer();
            recordingInterval = setInterval(function() {
                recordingSeconds++;
                updateRecordingTimer();
            }, 1000);
        })
        .catch(function(err) {
            console.error('Microphone error:', err);
            showToast('Không thể truy cập micro. Vui lòng cấp quyền sử dụng micro.', 'error');
        });
}

function stopRecording() {
    if (mediaRecorder && mediaRecorder.state === 'recording') {
        mediaRecorder.stop();
    }

    clearInterval(recordingInterval);

    document.getElementById('mic-button').classList.remove('recording');
    document.getElementById('mic-icon').classList.remove('hidden');
    document.getElementById('stop-icon').classList.add('hidden');
    document.getElementById('recording-wave').classList.add('hidden');
    document.getElementById('recording-timer').classList.add('hidden');
}

function updateRecordingTimer() {
    var m = Math.floor(recordingSeconds / 60);
    var s = recordingSeconds % 60;
    document.getElementById('recording-timer').textContent =
        (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;
}

function reRecord() {
    currentAudioBlob = null;
    document.getElementById('audio-playback').classList.add('hidden');
    document.getElementById('recording-status').textContent = 'Nhấn vào micro để ghi âm lại';
}

// ==================== WAV CONVERSION ====================
function convertToWav(blob) {
    return new Promise(function(resolve, reject) {
        var reader = new FileReader();
        reader.onload = function() {
            var audioContext = new (window.AudioContext || window.webkitAudioContext)();
            audioContext.decodeAudioData(reader.result)
                .then(function(audioBuffer) {
                    var wavBlob = audioBufferToWav(audioBuffer);
                    audioContext.close();
                    resolve(wavBlob);
                })
                .catch(function(err) {
                    reject(err);
                });
        };
        reader.onerror = function() { reject(reader.error); };
        reader.readAsArrayBuffer(blob);
    });
}

function audioBufferToWav(buffer) {
    var numChannels = 1; // mono
    var sampleRate = buffer.sampleRate;
    var format = 1; // PCM
    var bitsPerSample = 16;

    // Mix down to mono
    var channelData;
    if (buffer.numberOfChannels === 1) {
        channelData = buffer.getChannelData(0);
    } else {
        channelData = new Float32Array(buffer.length);
        for (var ch = 0; ch < buffer.numberOfChannels; ch++) {
            var data = buffer.getChannelData(ch);
            for (var i = 0; i < buffer.length; i++) {
                channelData[i] += data[i];
            }
        }
        for (var i = 0; i < buffer.length; i++) {
            channelData[i] /= buffer.numberOfChannels;
        }
    }

    var dataLength = channelData.length * (bitsPerSample / 8);
    var headerLength = 44;
    var totalLength = headerLength + dataLength;
    var arrayBuffer = new ArrayBuffer(totalLength);
    var view = new DataView(arrayBuffer);

    // WAV header
    writeString(view, 0, 'RIFF');
    view.setUint32(4, totalLength - 8, true);
    writeString(view, 8, 'WAVE');
    writeString(view, 12, 'fmt ');
    view.setUint32(16, 16, true); // chunk size
    view.setUint16(20, format, true);
    view.setUint16(22, numChannels, true);
    view.setUint32(24, sampleRate, true);
    view.setUint32(28, sampleRate * numChannels * bitsPerSample / 8, true);
    view.setUint16(32, numChannels * bitsPerSample / 8, true);
    view.setUint16(34, bitsPerSample, true);
    writeString(view, 36, 'data');
    view.setUint32(40, dataLength, true);

    // PCM data
    var offset = 44;
    for (var i = 0; i < channelData.length; i++) {
        var sample = Math.max(-1, Math.min(1, channelData[i]));
        var intSample = sample < 0 ? sample * 0x8000 : sample * 0x7FFF;
        view.setInt16(offset, intSample, true);
        offset += 2;
    }

    return new Blob([arrayBuffer], { type: 'audio/wav' });
}

function writeString(view, offset, str) {
    for (var i = 0; i < str.length; i++) {
        view.setUint8(offset + i, str.charCodeAt(i));
    }
}

// ==================== SUBMIT ANSWER ====================
function submitCurrentAnswer() {
    if (!currentAudioBlob) {
        showToast('Vui lòng ghi âm câu trả lời trước khi nộp.', 'warning');
        return;
    }

    // Kiểm tra thời lượng ghi âm tối thiểu (tránh gửi audio quá ngắn gây lỗi Whisper)
    if (recordingSeconds < 1) {
        showToast('Bản ghi âm quá ngắn. Vui lòng ghi âm ít nhất 1 giây.', 'warning');
        return;
    }

    var q = examQuestions[currentQuestionIndex];
    if (!q) return;

    // Show submitting UI
    document.getElementById('recording-section').classList.add('hidden');
    document.getElementById('submitting-section').classList.remove('hidden');
    document.getElementById('feedback-section').classList.add('hidden');

    var formData = new FormData();
    formData.append('questionId', q.questionId);
    formData.append('audio', currentAudioBlob, 'recording.wav');

    apiPostMultipart('/test-sessions/' + testSession.id + '/submit-answer', formData)
        .then(function(answer) {
            // Server now returns immediately with TRANSCRIBING status
            // Store the answer reference (will be updated by polling)
            if (answer.processingStatus === 'COMPLETED') {
                answersMap[q.questionId] = answer;
                updateProgress();
                renderQuestionNav();
                showFeedback(answer);
                showToast('Chấm điểm thành công!', 'success');
            } else if (answer.processingStatus === 'FAILED') {
                answersMap[q.questionId] = answer;
                updateProgress();
                renderQuestionNav();
                showFeedback(answer);
                showToast('AI không thể chấm điểm câu này.', 'warning');
            } else {
                // TRANSCRIBING / SCORING / PENDING - poll for result
                showToast('Đã nộp câu trả lời. AI đang xử lý...', 'info');
                pollAnswerResult(answer.id, q.questionId);
            }
        })
        .catch(function(err) {
            console.error(err);
            showToast('Lỗi khi nộp câu trả lời. Vui lòng thử lại.', 'error');
            showRecordingUI();
        });
}

function pollAnswerResult(answerId, questionId) {
    var attempts = 0;
    var maxAttempts = 120; // 120 * 3s = 6 minutes max

    var statusMessages = [
        'AI đang nhận dạng giọng nói...',
        'Đang phân tích nội dung câu trả lời...',
        'Đang chấm điểm với AI...',
        'Gần xong rồi, vui lòng chờ...'
    ];

    var poll = setInterval(function() {
        attempts++;

        // Update status message
        var msgIdx = Math.min(Math.floor(attempts / 5), statusMessages.length - 1);
        var detailEl = document.querySelector('.submitting-detail');
        if (detailEl) detailEl.textContent = statusMessages[msgIdx];

        if (attempts > maxAttempts) {
            clearInterval(poll);
            showToast('Quá thời gian chờ xử lý. Bạn có thể thử lại sau.', 'warning');
            showRecordingUI();
            return;
        }

        apiGet('/test-sessions/' + testSession.id + '/answers')
            .then(function(answers) {
                var answer = null;
                for (var i = 0; i < answers.length; i++) {
                    if (answers[i].questionId === questionId) {
                        answer = answers[i];
                        break;
                    }
                }
                if (answer && answer.processingStatus === 'COMPLETED') {
                    clearInterval(poll);
                    answersMap[questionId] = answer;
                    updateProgress();
                    renderQuestionNav();
                    // Only show feedback if still on the same question
                    if (examQuestions[currentQuestionIndex] && examQuestions[currentQuestionIndex].questionId === questionId) {
                        showFeedback(answer);
                    }
                    showToast('Chấm điểm thành công!', 'success');
                } else if (answer && answer.processingStatus === 'FAILED') {
                    clearInterval(poll);
                    answersMap[questionId] = answer;
                    updateProgress();
                    renderQuestionNav();
                    if (examQuestions[currentQuestionIndex] && examQuestions[currentQuestionIndex].questionId === questionId) {
                        showFeedback(answer);
                    }
                    showToast('AI không thể chấm điểm câu này. Bạn có thể ghi âm lại.', 'warning');
                }
            })
            .catch(function() {
                // Ignore polling errors
            });
    }, 3000);
}

// ==================== FINISH EXAM ====================
function confirmFinishExam() {
    var answered = Object.keys(answersMap).length;
    var total = examQuestions.length;
    var unanswered = total - answered;

    var msg;
    if (unanswered > 0) {
        msg = 'Bạn còn ' + unanswered + ' câu chưa trả lời. Bạn có chắc chắn muốn nộp bài?';
    } else {
        msg = 'Bạn đã trả lời tất cả ' + total + ' câu hỏi. Xác nhận nộp bài?';
    }

    document.getElementById('modal-message').textContent = msg;
    document.getElementById('modal-overlay').classList.remove('hidden');
}

function closeModal() {
    document.getElementById('modal-overlay').classList.add('hidden');
}

function finishExam() {
    closeModal();

    // Complete test session
    fetch(API_BASE + '/test-sessions/' + testSession.id + '/complete', { method: 'POST' })
        .then(function() {
            return apiGet('/reports/test-session/' + testSession.id + '/detailed');
        })
        .then(function(report) {
            showResults(report);
        })
        .catch(function(err) {
            console.error(err);
            // Still show results from local data
            showResultsFromLocal();
        });
}

function showResults(report) {
    document.getElementById('results-student-info').textContent =
        (report.studentName || '') + (report.studentOrganization ? ' - ' + report.studentOrganization : '') +
        ' | ' + (report.examName || '');

    document.getElementById('results-total-score').textContent =
        report.totalScore != null ? parseFloat(report.totalScore).toFixed(1) : '0';
    document.getElementById('results-answered').textContent = report.completedAnswers || 0;
    document.getElementById('results-total').textContent = report.totalQuestions || 0;
    document.getElementById('results-completion').textContent =
        (report.completionRate != null ? parseFloat(report.completionRate).toFixed(0) : 0) + '%';

    var answers = report.answers || [];
    renderResultsList(answers);
    goToScreen('screen-results');
}

function showResultsFromLocal() {
    var studentName = document.getElementById('student-name').value.trim();
    var org = document.getElementById('student-org').value.trim();

    document.getElementById('results-student-info').textContent =
        studentName + (org ? ' - ' + org : '') + ' | ' + selectedExam.name;

    var answered = Object.keys(answersMap).length;
    var total = examQuestions.length;
    var totalScore = 0;
    var scores = Object.values(answersMap);
    for (var i = 0; i < scores.length; i++) {
        totalScore += parseFloat(scores[i].score) || 0;
    }

    document.getElementById('results-total-score').textContent = totalScore.toFixed(1);
    document.getElementById('results-answered').textContent = answered;
    document.getElementById('results-total').textContent = total;
    document.getElementById('results-completion').textContent =
        (total > 0 ? (answered / total * 100).toFixed(0) : 0) + '%';

    // Build answers list from local data
    var answersList = [];
    for (var i = 0; i < examQuestions.length; i++) {
        var q = examQuestions[i];
        var a = answersMap[q.questionId];
        answersList.push({
            questionContent: q.questionContent,
            transcribedText: a ? a.transcribedText : null,
            score: a ? a.score : null,
            feedback: a ? a.feedback : null,
            status: a ? 'COMPLETED' : 'NOT_ANSWERED'
        });
    }
    renderResultsList(answersList);
    goToScreen('screen-results');
}

function renderResultsList(answers) {
    var container = document.getElementById('results-answers-list');
    var html = '';

    for (var i = 0; i < answers.length; i++) {
        var a = answers[i];
        var delay = i * 0.05;
        var isAnswered = a.status === 'COMPLETED' || (a.score != null && a.score !== undefined);

        html += '<div class="result-answer-card" style="animation-delay: ' + delay + 's">' +
            '<div class="result-q-num ' + (isAnswered ? 'answered' : 'unanswered') + '">' + (i + 1) + '</div>' +
            '<div class="result-content">' +
                '<div class="result-question">' + escapeHtml(a.questionContent || 'Câu ' + (i + 1)) + '</div>';

        if (isAnswered) {
            html += '<div class="result-transcript">"' + escapeHtml(a.transcribedText || '') + '"</div>';
            if (a.feedback) {
                html += '<div class="result-feedback">' + escapeHtml(a.feedback) + '</div>';
            }
        } else {
            html += '<div class="result-not-answered">Chưa trả lời</div>';
        }

        html += '</div>';

        if (isAnswered) {
            var score = parseFloat(a.score) || 0;
            html += '<div class="result-score-badge">' + score.toFixed(1) + '</div>';
        }

        html += '</div>';
    }

    // If answers is empty but we have exam questions, fill from examQuestions
    if (answers.length === 0 && examQuestions.length > 0) {
        for (var i = 0; i < examQuestions.length; i++) {
            var q = examQuestions[i];
            var a = answersMap[q.questionId];
            var isAns = a !== undefined;
            html += '<div class="result-answer-card">' +
                '<div class="result-q-num ' + (isAns ? 'answered' : 'unanswered') + '">' + (i + 1) + '</div>' +
                '<div class="result-content">' +
                    '<div class="result-question">' + escapeHtml(q.questionContent) + '</div>' +
                    (isAns ? '<div class="result-transcript">"' + escapeHtml(a.transcribedText || '') + '"</div>' : '<div class="result-not-answered">Chưa trả lời</div>') +
                '</div>' +
                (isAns ? '<div class="result-score-badge">' + (parseFloat(a.score) || 0).toFixed(1) + '</div>' : '') +
            '</div>';
        }
    }

    container.innerHTML = html;
}

// ==================== RESET ====================
function resetAll() {
    selectedExam = null;
    testSession = null;
    examQuestions = [];
    currentQuestionIndex = 0;
    answersMap = {};
    currentAudioBlob = null;

    document.getElementById('student-name').value = '';
    document.getElementById('student-org').value = '';
    document.getElementById('exam-search').value = '';

    // Stop any recording
    if (mediaRecorder && mediaRecorder.state === 'recording') {
        mediaRecorder.stop();
    }
    if (recordingStream) {
        recordingStream.getTracks().forEach(function(t) { t.stop(); });
    }
    clearInterval(recordingInterval);

    loadExams(0);
}

// ==================== UTILITIES ====================
function escapeHtml(str) {
    if (!str) return '';
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(str));
    return div.innerHTML;
}

// ==================== INITIALIZATION ====================
document.addEventListener('DOMContentLoaded', function() {
    loadExams(0);
});
