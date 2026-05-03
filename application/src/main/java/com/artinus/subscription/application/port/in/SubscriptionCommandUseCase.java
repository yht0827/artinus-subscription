package com.artinus.subscription.application.port.in;

import com.artinus.subscription.application.command.CancelCommand;
import com.artinus.subscription.application.command.SubscribeCommand;
import com.artinus.subscription.application.result.SubscriptionResult;

public interface SubscriptionCommandUseCase {

	SubscriptionResult subscribe(SubscribeCommand command);

	SubscriptionResult cancel(CancelCommand command);
}
