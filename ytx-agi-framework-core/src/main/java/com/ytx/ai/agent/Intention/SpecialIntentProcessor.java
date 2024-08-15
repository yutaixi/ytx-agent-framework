package com.ytx.ai.agent.Intention;

import com.ytx.ai.agent.vo.UserIntention;

public interface SpecialIntentProcessor {

    public UserIntention process(String question);

}
