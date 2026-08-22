package dev.soham.ai.springailanggraphhitl;

import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;

public class OtpAuthenticationFlowState extends AgentState {

    public static final String PHONE_NUMBER = "phoneNumber";
    public static final String OTP = "otp";
    public static final String USER_OTP_INPUT = "userOtpInput";
    public static final String USER_PHONE_INPUT = "userMessagePhone";
    public static final String USER_OTP ="userOtp" ;
    public static final String RESULT = "result";
    public static final String RETRY_COUNT = "retryCount";
    public static final String CONTROLLER_MESSAGE = "controllerMessage";

    public OtpAuthenticationFlowState(Map<String, Object> initData) {
        super(initData);
    }

    public String getPhoneNumber() {
        return this.<String>value(PHONE_NUMBER).orElse("");
    }

    public String getOtp() {
        return this.<String>value(OTP).orElse("");
    }

    public String getUserOtpInput() {
        return this.<String>value(USER_OTP_INPUT).orElse("");
    }
    public String getUserMessagePhone(){
        return this.<String>value(USER_PHONE_INPUT).orElse("");
    }

    public String userOtp(){
        return this.<String>value(USER_OTP).orElse("");
    }

    public String result(){
        return this.<String>value(RESULT).orElse("");
    }

    public int retryCount() {
        return this.<Integer>value(RETRY_COUNT).orElse(0);
    }



}
