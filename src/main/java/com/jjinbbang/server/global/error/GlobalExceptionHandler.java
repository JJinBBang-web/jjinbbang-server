package com.jjinbbang.server.global.error;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.jjinbbang.server.global.error.dto.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * 모든 실패 응답이 여기 한 곳을 지난다. 상태 코드 매핑을 {@code global}에 모아두는 Ver.1의 방식은
 * 그대로 두고, 도메인이 상태별 예외 클래스를 만들지 않게 한 것만 달라졌다.
 *
 * <p>도메인 사유는 전부 {@link BusinessException} 하나로 올라오고, 상태는 {@link ErrorCode#getStatus()}가 정한다.
 * 따라서 <b>새 도메인 에러를 추가할 때 이 파일을 고칠 일이 없다.</b>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/** 도메인 예외 — ErrorCode가 상태·코드값·메시지를 모두 들고 있다. */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
		ErrorCode errorCode = e.getErrorCode();
		HttpStatus status = errorCode.getStatus();

		if (status.is5xxServerError()) {
			log.error("[{}] {} - {}", status.value(), errorCode.getCode(), e.getMessage(), e);
		} else {
			log.warn("[{}] {} - {}", status.value(), errorCode.getCode(), e.getMessage());
		}

		return ResponseEntity.status(status)
			.body(ErrorResponse.of(errorCode, e.getMessage()));
	}

	/** 요청 DTO 검증 실패 — 필드별로 전부 내려준다. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
		List<ErrorResponse.FieldError> errors = e.getBindingResult().getFieldErrors().stream()
			.map(fieldError -> new ErrorResponse.FieldError(fieldError.getField(), fieldError.getDefaultMessage()))
			.toList();

		log.warn("[400] {} - {}", GlobalErrorCode.INVALID_INPUT.getCode(), errors);

		return ResponseEntity.status(GlobalErrorCode.INVALID_INPUT.getStatus())
			.body(ErrorResponse.of(GlobalErrorCode.INVALID_INPUT, GlobalErrorCode.INVALID_INPUT.getMessage(), errors));
	}

	/** 경로·쿼리 파라미터 타입 불일치 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
		String message = String.format(GlobalErrorCode.INVALID_TYPE.getMessage(), e.getName());
		log.warn("[400] {} - {}", GlobalErrorCode.INVALID_TYPE.getCode(), message);

		return ResponseEntity.status(GlobalErrorCode.INVALID_TYPE.getStatus())
			.body(ErrorResponse.of(GlobalErrorCode.INVALID_TYPE, message));
	}

	/** 본문 파싱 실패 (JSON 형식 오류 등) */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
		log.warn("[400] {} - {}", GlobalErrorCode.INVALID_INPUT.getCode(), e.getMessage());

		return ResponseEntity.status(GlobalErrorCode.INVALID_INPUT.getStatus())
			.body(ErrorResponse.of(GlobalErrorCode.INVALID_INPUT, GlobalErrorCode.INVALID_INPUT.getMessage()));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
		log.warn("[405] {} - {}", GlobalErrorCode.METHOD_NOT_ALLOWED.getCode(), e.getMessage());

		return ResponseEntity.status(GlobalErrorCode.METHOD_NOT_ALLOWED.getStatus())
			.body(ErrorResponse.of(GlobalErrorCode.METHOD_NOT_ALLOWED, GlobalErrorCode.METHOD_NOT_ALLOWED.getMessage()));
	}

	/** 매핑되지 않은 경로 — 아래 catch-all이 500으로 바꿔버리지 않게 먼저 잡는다. */
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e) {
		log.warn("[404] {} - {}", GlobalErrorCode.ENDPOINT_NOT_FOUND.getCode(), e.getResourcePath());

		return ResponseEntity.status(GlobalErrorCode.ENDPOINT_NOT_FOUND.getStatus())
			.body(ErrorResponse.of(GlobalErrorCode.ENDPOINT_NOT_FOUND, GlobalErrorCode.ENDPOINT_NOT_FOUND.getMessage()));
	}

	/**
	 * 예상하지 못한 예외. Ver.1은 스택 트레이스를 남기지 않아 500 원인 추적이 어려웠으므로 여기서는 남긴다.
	 * 대신 응답에는 내부 사정을 노출하지 않고 고정 메시지를 쓴다.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e) {
		log.error("[500] {} - {}", GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(), e.getMessage(), e);

		return ResponseEntity.status(GlobalErrorCode.INTERNAL_SERVER_ERROR.getStatus())
			.body(ErrorResponse.of(GlobalErrorCode.INTERNAL_SERVER_ERROR,
				GlobalErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
	}
}
