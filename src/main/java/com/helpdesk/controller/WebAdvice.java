package com.helpdesk.controller;

import java.beans.PropertyEditorSupport;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;

import com.helpdesk.exception.BusinessException;
import com.helpdesk.service.CurrentUserService;

@ControllerAdvice
public class WebAdvice {
    private final CurrentUserService users;

    public WebAdvice(CurrentUserService users) {
        this.users = users;
    }

    @InitBinder
    public void trim(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        // 密碼中的首尾空白也屬於密碼內容，必須排除一般文字欄位的 trim 行為。
        for (String field : new String[] {"password", "oldPassword", "newPassword"}) {
            binder.registerCustomEditor(
                    String.class,
                    field,
                    new PropertyEditorSupport() {
                        @Override
                        public void setAsText(String text) {
                            setValue(text);
                        }
                    });
        }
    }

    @ModelAttribute
    public void actor(Model model, HttpServletRequest request) {
        if (request.getUserPrincipal() != null) {
            try {
                model.addAttribute("actor", users.current());
            } catch (AccessDeniedException ignored) {
                /* Security filter 拒絕失效帳號。 */
            }
        }
    }

    private ModelAndView error(HttpStatus status, String message) {
        ModelAndView view = new ModelAndView("error/message");
        view.setStatus(status);
        view.addObject("message", message);
        view.addObject("code", status.value());
        return view;
    }

    @ExceptionHandler(BusinessException.class)
    public ModelAndView business(BusinessException ex) {
        return error(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView forbidden(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "您沒有存取這項資料或操作的權限");
    }

    @ExceptionHandler({
        org.springframework.dao.OptimisticLockingFailureException.class,
        jakarta.persistence.OptimisticLockException.class
    })
    public ModelAndView stale(Exception ex) {
        return error(HttpStatus.CONFLICT, "案件已更新，請重新整理後再操作");
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ModelAndView integrity(Exception ex) {
        return error(HttpStatus.CONFLICT, "資料已被使用或重複，請重新檢查輸入內容");
    }

    @ExceptionHandler(org.springframework.orm.jpa.JpaSystemException.class)
    public ModelAndView persistence(org.springframework.orm.jpa.JpaSystemException ex) {
        String detail =
                org.springframework.core.NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
        if (detail != null && detail.contains("SQLITE_CONSTRAINT")) {
            return error(HttpStatus.CONFLICT, "資料寫入未完成，本次變更已取消，請重新檢查資料");
        }
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "資料庫暫時無法完成操作，請稍後重試");
    }

    @ExceptionHandler(org.springframework.dao.CannotAcquireLockException.class)
    public ModelAndView busy(Exception ex) {
        return error(HttpStatus.CONFLICT, "資料正在更新，請重新整理後再操作");
    }

    @ExceptionHandler({
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        org.springframework.validation.BindException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class
    })
    public ModelAndView invalid(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "輸入格式不正確，請檢查欄位內容");
    }
}
