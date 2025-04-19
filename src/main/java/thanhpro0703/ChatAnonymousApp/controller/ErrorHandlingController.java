package thanhpro0703.ChatAnonymousApp.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class ErrorHandlingController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        
        log.error("Error occurred: status={}, message={}", status, message);
        
        if (exception != null) {
            log.error("Exception details: {}", exception);
        }
        
        model.addAttribute("errorMessage", "Đã xảy ra lỗi! (Mã: " + status + ")");
        model.addAttribute("errorDetails", message != null ? message.toString() : "Vui lòng thử lại sau hoặc liên hệ quản trị viên.");
        
        return "error-simple";
    }
    
    @GetMapping("/api/error")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleApiError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("status", status != null ? status : "unknown");
        errorDetails.put("error", "An error occurred");
        errorDetails.put("message", message != null ? message : "No details available");
        
        if (exception != null) {
            log.error("Exception occurred: {}", exception);
            errorDetails.put("exception", exception.toString());
        }
        
        log.error("API Error details: {}", errorDetails);
        
        return new ResponseEntity<>(errorDetails, 
                status != null ? HttpStatus.valueOf(Integer.parseInt(status.toString())) : HttpStatus.INTERNAL_SERVER_ERROR);
    }
} 