package main.com.lcc.hahu.service;

import main.com.lcc.hahu.mapper.*;
import main.com.lcc.hahu.model.Question;
import main.com.lcc.hahu.util.MyUtil;
import main.com.lcc.hahu.util.RedisKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Created by liangchengcheng on 2017/7/7.
 */
@Service
public class QuestionService {

    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private AnswerMapper answerMapper;
    @Autowired
    private TopicMapper topicMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private JedisPool jedisPool;

    /**
     * 关注收藏夹
     */
    public void followQuestion(Integer userId, Integer questionId) {
        Jedis jedis = jedisPool.getResource();
        jedis.zadd(userId + RedisKey.FOLLOW_QUESTION, new Date().getTime(), String.valueOf(questionId));
        jedis.zadd(questionId + RedisKey.FOLLOWED_QUESTION, new Date().getTime(), String.valueOf(userId));
        jedisPool.returnResource(jedis);
    }

    /**
     * 取消关注收藏夹
     */
    public void unfollowQuestion(Integer userId, Integer questionId) {
        Jedis jedis = jedisPool.getResource();
        jedis.zrem(userId + RedisKey.FOLLOW_QUESTION, String.valueOf(questionId));
        jedis.zrem(questionId + RedisKey.FOLLOWED_QUESTION, String.valueOf(userId));
        jedisPool.returnResource(jedis);
    }

    /**
     * 列出所关注的问题
     */
    public List<Question> listFollowingQuestion(Integer userId){
        Jedis jedis = jedisPool.getResource();
        //获取所关注问题的id的集合
        Set<String> idSet = jedis.zrange(userId+ RedisKey.FOLLOW_QUESTION,0,-1);
        List<Integer> idList = MyUtil.StringSetToIntegerList(idSet);

        List<Question> list = new ArrayList<>();
        if (idList.size() > 0){
            list = questionMapper.listQuestionByQuestionId(idList);
            for (Question question :list){
                //设置回答的数目
                int answerCount = answerMapper.selectAnswerCountByQuestionId(question.getQuestionId());
                question.setAnswerCount(answerCount);
                Long followedCount = jedis.zcard(question.getQuestionId() + RedisKey.FOLLOWED_QUESTION);
                question.setFollowedCount(Integer.parseInt(followedCount + ""));
            }
        }
        jedisPool.returnResource(jedis);
        return list;
    }
}
