package thanhpro0703.ChatAnonymousApp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Controller
public class StaticContentController {

    @GetMapping(value = "/static-index", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String serveIndex() throws IOException {
        // Đọc nội dung file index.html
        Resource resource = new ClassPathResource("templates/index.html");
        byte[] bytes = Files.readAllBytes(resource.getFile().toPath());
        String content = new String(bytes, StandardCharsets.UTF_8);
        
        // Sửa URL WebSocket
        content = content.replace("const wsUrl = `${wsProtocol}//${window.location.host}/ws/chat`", 
                                "const wsUrl = `${wsProtocol}//${window.location.host}/chat`");
        
        return content;
    }

    @GetMapping(value = "/fixed-index", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String fixedIndex() {
        return "<!DOCTYPE html>\n" +
               "<html lang=\"vi\">\n" +
               "<head>\n" +
               "    <meta charset=\"UTF-8\">\n" +
               "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
               "    <title>Chat Ẩn Danh | Phiên bản đơn giản</title>\n" +
               "    <script src=\"https://cdn.tailwindcss.com\"></script>\n" +
               "    <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css\">\n" +
               "</head>\n" +
               "<body class=\"bg-gray-100 min-h-screen p-6\">\n" +
               "    <div class=\"max-w-xl mx-auto bg-white rounded-lg shadow-lg p-6\">\n" +
               "        <h1 class=\"text-2xl font-bold text-center mb-6\">Chat Ẩn Danh</h1>\n" +
               "        <div class=\"p-4 bg-yellow-100 text-yellow-800 rounded-lg text-center mb-6\">\n" +
               "            <p>Cập nhật: Đã sửa lỗi kết nối liên tục và ghép cặp người dùng</p>\n" +
               "        </div>\n" +
               "        \n" +
               "        <div class=\"p-4 bg-red-100 text-red-800 rounded-lg text-center mb-6\">\n" +
               "            <p><strong>Lưu ý:</strong> Nếu bạn tải lại trang trong lúc đang trò chuyện, bạn sẽ mất kết nối với đối phương và phải tìm người mới.</p>\n" +
               "        </div>\n" +
               "        \n" +
               "        <div id=\"status\" class=\"p-4 bg-blue-100 text-blue-800 rounded-lg text-center mb-6\">\n" +
               "            Đang kết nối...\n" +
               "        </div>\n" +
               "        \n" +
               "        <div id=\"chat-container\" class=\"h-80 border rounded-lg p-4 mb-4 overflow-y-auto hidden\">\n" +
               "            <!-- Messages will be displayed here -->\n" +
               "        </div>\n" +
               "        \n" +
               "        <div id=\"input-form\" class=\"flex space-x-2 mb-4 hidden\">\n" +
               "            <input type=\"text\" id=\"message-input\" class=\"flex-1 border rounded-lg px-4 py-2\" placeholder=\"Nhập tin nhắn...\">\n" +
               "            <button id=\"send-btn\" class=\"bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700\">\n" +
               "                <i class=\"fas fa-paper-plane\"></i>\n" +
               "            </button>\n" +
               "        </div>\n" +
               "        \n" +
               "        <div class=\"flex justify-center space-x-4\">\n" +
               "            <button id=\"find-btn\" class=\"bg-purple-600 text-white px-4 py-2 rounded-lg hover:bg-purple-700\">\n" +
               "                <i class=\"fas fa-random mr-2\"></i> Tìm người trò chuyện\n" +
               "            </button>\n" +
               "            \n" +
               "            <button id=\"leave-btn\" class=\"bg-gray-600 text-white px-4 py-2 rounded-lg hover:bg-gray-700 hidden\">\n" +
               "                <i class=\"fas fa-sign-out-alt mr-2\"></i> Rời trò chuyện\n" +
               "            </button>\n" +
               "        </div>\n" +
               "    </div>\n" +
               "    \n" +
               "    <script>\n" +
               "        // Elements\n" +
               "        const statusEl = document.getElementById('status');\n" +
               "        const chatContainer = document.getElementById('chat-container');\n" +
               "        const inputForm = document.getElementById('input-form');\n" +
               "        const messageInput = document.getElementById('message-input');\n" +
               "        const sendBtn = document.getElementById('send-btn');\n" +
               "        const findBtn = document.getElementById('find-btn');\n" +
               "        const leaveBtn = document.getElementById('leave-btn');\n" +
               "        \n" +
               "        // Variables\n" +
               "        const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';\n" +
               "        const wsUrl = `${wsProtocol}//${window.location.host}/chat`;\n" +
               "        let socket;\n" +
               "        let partnerId = null;\n" +
               "        let isReconnecting = false; // Biến theo dõi trạng thái kết nối lại\n" +
               "        let mySessionId = localStorage.getItem('chatSessionId') || null;\n" +
               "        \n" +
               "        // Phát hiện reload trang\n" +
               "        const pageAccessedByReload = (\n" +
               "            (window.performance.navigation && window.performance.navigation.type === 1) ||\n" +
               "            window.performance.getEntriesByType('navigation')\n" +
               "                .map((nav) => nav.type)\n" +
               "                .includes('reload')\n" +
               "        );\n" +
               "        \n" +
               "        if (pageAccessedByReload && mySessionId) {\n" +
               "            console.log('Trang được tải lại, đang khôi phục phiên ' + mySessionId);\n" +
               "            isReconnecting = true;\n" +
               "            // Khôi phục partnerId nếu có\n" +
               "            const savedPartnerId = localStorage.getItem('chatPartnerId');\n" +
               "            if (savedPartnerId) {\n" +
               "                partnerId = savedPartnerId;\n" +
               "                console.log('Khôi phục kết nối với partner: ' + partnerId);\n" +
               "            }\n" +
               "        }\n" +
               "        \n" +
               "        // Initialize\n" +
               "        findBtn.addEventListener('click', findPartner);\n" +
               "        leaveBtn.addEventListener('click', leaveChat);\n" +
               "        sendBtn.addEventListener('click', sendMessage);\n" +
               "        messageInput.addEventListener('keypress', e => {\n" +
               "            if (e.key === 'Enter') sendMessage();\n" +
               "        });\n" +
               "        \n" +
               "        function connectWebSocket() {\n" +
               "            updateStatus('Đang kết nối...', 'blue');\n" +
               "            \n" +
               "            try {\n" +
               "                socket = new WebSocket(wsUrl);\n" +
               "                \n" +
               "                socket.onopen = () => {\n" +
               "                    updateStatus('Đã kết nối! Đang tìm kiếm người trò chuyện...', 'green');\n" +
               "                    // Luôn tự động tìm người khi kết nối\n" +
               "                    findPartner();\n" +
               "                };\n" +
               "                \n" +
               "                socket.onmessage = (event) => {\n" +
               "                    const data = JSON.parse(event.data);\n" +
               "                    console.log('Received:', data);\n" +
               "                    \n" +
               "                    // Nếu chưa có sessionId, lấy từ response đầu tiên\n" +
               "                    if (!mySessionId && data.sessionId) {\n" +
               "                        mySessionId = data.sessionId;\n" +
               "                        console.log('Đã lưu sessionId:', mySessionId);\n" +
               "                        localStorage.setItem('chatSessionId', mySessionId);\n" +
               "                    }\n" +
               "                    \n" +
               "                    switch (data.type) {\n" +
               "                        case 'connected':\n" +
               "                            partnerId = data.senderId;\n" +
               "                            // Lưu thông tin partnerId để khôi phục sau khi reload\n" +
               "                            localStorage.setItem('chatPartnerId', partnerId);\n" +
               "                            updateStatus('Đã kết nối với một người lạ!', 'green');\n" +
               "                            showChat();\n" +
               "                            break;\n" +
               "                            \n" +
               "                        case 'leave':\n" +
               "                            updateStatus('Người trò chuyện đã ngắt kết nối!', 'yellow');\n" +
               "                            hideChat();\n" +
               "                            partnerId = null;\n" +
               "                            // Xóa partnerId trong localStorage\n" +
               "                            localStorage.removeItem('chatPartnerId');\n" +
               "                            break;\n" +
               "                            \n" +
               "                        case 'ping':\n" +
               "                            // Phản hồi lại server để giữ kết nối WebSocket\n" +
               "                            console.log('Received ping from server');\n" +
               "                            socket.send(JSON.stringify({type: 'pong'}));\n" +
               "                            break;\n" +
               "                            \n" +
               "                        case 'message':\n" +
               "                            if (data.sender === 'stranger') {\n" +
               "                                appendMessage(data.content, 'partner');\n" +
               "                            }\n" +
               "                            break;\n" +
               "                            \n" +
               "                        case 'status':\n" +
               "                            updateStatus(data.content, 'blue');\n" +
               "                            if (data.content.includes('chờ kết nối')) {\n" +
               "                                hideChat();\n" +
               "                                partnerId = null;\n" +
               "                            }\n" +
               "                            break;\n" +
               "                    }\n" +
               "                };\n" +
               "                \n" +
               "                socket.onclose = () => {\n" +
               "                    updateStatus('Mất kết nối đến máy chủ! Đang kết nối lại...', 'red');\n" +
               "                    setTimeout(connectWebSocket, 3000);\n" +
               "                };\n" +
               "                \n" +
               "                socket.onerror = () => {\n" +
               "                    updateStatus('Lỗi kết nối! Đang thử lại...', 'red');\n" +
               "                };\n" +
               "                \n" +
               "            } catch (error) {\n" +
               "                updateStatus('Không thể kết nối! Đang thử lại...', 'red');\n" +
               "                setTimeout(connectWebSocket, 3000);\n" +
               "            }\n" +
               "        }\n" +
               "        \n" +
               "        function updateStatus(message, color) {\n" +
               "            statusEl.textContent = message;\n" +
               "            \n" +
               "            // Update color\n" +
               "            statusEl.className = `p-4 rounded-lg text-center mb-6`;\n" +
               "            switch (color) {\n" +
               "                case 'green':\n" +
               "                    statusEl.classList.add('bg-green-100', 'text-green-800');\n" +
               "                    break;\n" +
               "                case 'red':\n" +
               "                    statusEl.classList.add('bg-red-100', 'text-red-800');\n" +
               "                    break;\n" +
               "                case 'yellow':\n" +
               "                    statusEl.classList.add('bg-yellow-100', 'text-yellow-800');\n" +
               "                    break;\n" +
               "                default:\n" +
               "                    statusEl.classList.add('bg-blue-100', 'text-blue-800');\n" +
               "                    break;\n" +
               "            }\n" +
               "        }\n" +
               "        \n" +
               "        function findPartner() {\n" +
               "            if (socket && socket.readyState === WebSocket.OPEN) {\n" +
               "                // Kiểm tra nếu đã kết nối, không tìm lại\n" +
               "                if (partnerId != null) {\n" +
               "                    console.log('Already connected to a partner, not searching again');\n" +
               "                    return;\n" +
               "                }\n" +
               "                \n" +
               "                console.log('Sending find partner request');\n" +
               "                socket.send(JSON.stringify({type: 'find'}));\n" +
               "                updateStatus('Đang tìm người trò chuyện...', 'blue');\n" +
               "            } else {\n" +
               "                console.log('Socket not ready, connecting first');\n" +
               "                connectWebSocket();\n" +
               "            }\n" +
               "        }\n" +
               "        \n" +
               "        function leaveChat() {\n" +
               "            if (socket && socket.readyState === WebSocket.OPEN && partnerId) {\n" +
               "                socket.send(JSON.stringify({type: 'leave'}));\n" +
               "                hideChat();\n" +
               "                updateStatus('Bạn đã rời khỏi cuộc trò chuyện', 'blue');\n" +
               "                partnerId = null;\n" +
               "            }\n" +
               "        }\n" +
               "        \n" +
               "        function sendMessage() {\n" +
               "            const content = messageInput.value.trim();\n" +
               "            if (content && socket && socket.readyState === WebSocket.OPEN && partnerId) {\n" +
               "                const message = {\n" +
               "                    type: 'message',\n" +
               "                    content: content,\n" +
               "                    receiver: partnerId\n" +
               "                };\n" +
               "                socket.send(JSON.stringify(message));\n" +
               "                appendMessage(content, 'self');\n" +
               "                messageInput.value = '';\n" +
               "            }\n" +
               "        }\n" +
               "        \n" +
               "        function appendMessage(content, sender) {\n" +
               "            const messageDiv = document.createElement('div');\n" +
               "            messageDiv.className = 'mb-3';\n" +
               "            \n" +
               "            if (sender === 'self') {\n" +
               "                messageDiv.innerHTML = `\n" +
               "                    <div class=\"flex justify-end\">\n" +
               "                        <div class=\"bg-blue-600 text-white px-4 py-2 rounded-lg max-w-xs\">\n" +
               "                            ${content}\n" +
               "                        </div>\n" +
               "                    </div>\n" +
               "                `;\n" +
               "            } else {\n" +
               "                messageDiv.innerHTML = `\n" +
               "                    <div class=\"flex justify-start\">\n" +
               "                        <div class=\"bg-gray-200 text-gray-800 px-4 py-2 rounded-lg max-w-xs\">\n" +
               "                            ${content}\n" +
               "                        </div>\n" +
               "                    </div>\n" +
               "                `;\n" +
               "            }\n" +
               "            \n" +
               "            chatContainer.appendChild(messageDiv);\n" +
               "            chatContainer.scrollTop = chatContainer.scrollHeight;\n" +
               "        }\n" +
               "        \n" +
               "        function showChat() {\n" +
               "            chatContainer.classList.remove('hidden');\n" +
               "            inputForm.classList.remove('hidden');\n" +
               "            leaveBtn.classList.remove('hidden');\n" +
               "            messageInput.focus();\n" +
               "        }\n" +
               "        \n" +
               "        function hideChat() {\n" +
               "            inputForm.classList.add('hidden');\n" +
               "            leaveBtn.classList.add('hidden');\n" +
               "            chatContainer.innerHTML = '';\n" +
               "        }\n" +
               "        \n" +
               "        // Hàm gửi thông tin khôi phục phiên\n" +
               "        function sendSessionRestore() {\n" +
               "            if (!socket || socket.readyState !== WebSocket.OPEN) return;\n" +
               "            \n" +
               "            const restoreMessage = {\n" +
               "                type: 'restore',\n" +
               "                oldSessionId: mySessionId,\n" +
               "                partnerId: partnerId\n" +
               "            };\n" +
               "            \n" +
               "            console.log('Gửi yêu cầu khôi phục phiên:', restoreMessage);\n" +
               "            socket.send(JSON.stringify(restoreMessage));\n" +
               "        }\n" +
               "        \n" +
               "        // Start connection when page loads\n" +
               "        connectWebSocket();\n" +
               "    </script>\n" +
               "</body>\n" +
               "</html>";
    }
} 