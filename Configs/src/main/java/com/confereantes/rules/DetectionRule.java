package com.confereantes.rules;

import java.util.List;
import com.confereantes.model.Message;

public interface DetectionRule {

    List<RuleMatch> evaluate(List<Message> messages);
}