package main.com.lcc.hahu.service;

import main.com.lcc.hahu.mapper.AnswerMapper;
import main.com.lcc.hahu.mapper.QuestionMapper;
import main.com.lcc.hahu.mapper.TopicMapper;
import main.com.lcc.hahu.model.Topic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by liangchengcheng on 2017/7/7.
 */

@Service
public class TopicService {
    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private AnswerMapper answerMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private JedisPool jedisPool;

    public Map<String,Object> listAllTopic(){
        Map<String,Object> map = new HashMap();
        List<Topic> hotTopicList = topicMapper.listHotTopic();
        List<Topic> rootTopicList = topicMapper.listRootTopic();
        map.put("hotTopicList", hotTopicList);
        map.put("rootTopicList", rootTopicList);

        return map;

    }

}
