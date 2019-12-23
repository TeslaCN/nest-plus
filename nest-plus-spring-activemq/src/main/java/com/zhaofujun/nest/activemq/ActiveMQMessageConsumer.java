package com.zhaofujun.nest.activemq;

import com.zhaofujun.nest.core.BeanFinder;
import com.zhaofujun.nest.core.EventData;
import com.zhaofujun.nest.core.EventHandler;
import com.zhaofujun.nest.context.event.message.MessageInfo;
import com.zhaofujun.nest.context.event.channel.distribute.DistributeMessageConsumer;
import com.zhaofujun.nest.json.JsonCreator;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.jms.core.JmsTemplate;


import javax.jms.*;

public class ActiveMQMessageConsumer extends DistributeMessageConsumer {

    private JmsTemplate jmsTemplate;
    private JsonCreator jsonCreator;
    private volatile boolean running = false;

    public ActiveMQMessageConsumer(JmsTemplate jmsTemplate, BeanFinder beanFinder) {
        super(beanFinder);
        jsonCreator=new JsonCreator(beanFinder);
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    public void subscribe(EventHandler eventHandler) {
        running = true;
        while (running) {
            for (int i = 0; i < 10; i++) {
                if (running) {
                    Queue queue = new ActiveMQQueue("Consumer." + eventHandler.getClass().getSimpleName() + ".VirtualTopic." + eventHandler.getEventCode());
                    Message message = jmsTemplate.receive(queue);
                    TextMessage textMessage = (TextMessage) message;
                    String messageText = null;
                    try {
                        messageText = textMessage.getText();
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                    MessageInfo messageInfo = jsonCreator.toObj(messageText, MessageInfo.class);
                    String eventDataJson = jsonCreator.toJsonString(messageInfo.getData());
                    Object o = jsonCreator.toObj(eventDataJson, eventHandler.getEventDataClass());
                    messageInfo.setData((EventData) o);
                    onReceivedMessage(messageInfo, eventHandler, null);
                }
            }
        }
    }

    @Override
    protected void onFailed(EventHandler eventHandler, Object context, Exception ex) {

    }

    @Override
    protected void onEnds(EventHandler eventHandler, Object context) {
    }


    @Override
    public void stop() {
        running = false;
    }
}
