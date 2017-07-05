package main.com.lcc.hahu.service;

import main.com.lcc.hahu.mapper.AnswerMapper;
import main.com.lcc.hahu.mapper.MessageMapper;
import main.com.lcc.hahu.mapper.QuestionMapper;
import main.com.lcc.hahu.mapper.UserMapper;
import main.com.lcc.hahu.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by liangchengcheng on 2017/7/6.
 */
@Service
public class MessageService {
    @Autowired
    private AnswerMapper answerMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MessageMapper messageMapper;

    public Map<String, List<Message>> listMessage(Integer userId) {
        List<Message> messageList = messageMapper.listMessageByUserId(userId);
        Map<String, List<Message>> map = new HashMap<>();
        for (Message message : messageList) {
            String time = message.getMessageDate();
            if (map.get(time) == null) {
                map.put(time, new LinkedList<Message>());
                map.get(time).add(message);
            } else {
                map.get(time).add(message);
            }
        }
        return map;
    }

}
