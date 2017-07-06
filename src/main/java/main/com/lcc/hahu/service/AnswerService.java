package main.com.lcc.hahu.service;

import main.com.lcc.hahu.mapper.AnswerMapper;
import main.com.lcc.hahu.mapper.MessageMapper;
import main.com.lcc.hahu.mapper.QuestionMapper;
import main.com.lcc.hahu.mapper.UserMapper;
import main.com.lcc.hahu.model.Answer;
import main.com.lcc.hahu.model.Message;
import main.com.lcc.hahu.model.PageBean;
import main.com.lcc.hahu.model.Question;
import main.com.lcc.hahu.util.MyUtil;
import main.com.lcc.hahu.util.RedisKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by liangchengcheng on 2017/7/6.
 */
@Service
public class AnswerService {

    @Autowired
    private AnswerMapper answerMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private JedisPool jedisPool;


    public Integer answer(Answer answer,Integer userId){
        answer.setUserId(userId);
        answer.setCreateTime(new Date().getTime());
        answerMapper.insertAnswer(answer);

        //插入一条点赞消息
        Message message = new Message();
        message.setType(Message.TYPE_ANSWER);
        message.setSecondType(1);
        Date date = new Date();
        message.setMessageDate(MyUtil.formatDate(date));
        message.setMessageTime(date.getTime());
        message.setFromUserId(userId);
        message.setFromUserName(userMapper.selectUsernameByUserId(userId));


        Question question = questionMapper.selectQuestionByAnswerId(answer.getAnswerId());
        message.setQuestionId(question.getQuestionId());
        message.setQuestionTitle(question.getQuestionTitle());
        message.setAnswerId(answer.getAnswerId());
        message.setUserId(question.getUserId());
        messageMapper.insertTypeComment(message);

        return answer.getAnswerId();
    }

    public PageBean<Answer> listAnswerByUserId(Integer userId, Integer curPage) {

        // 当请求页数为空时
        curPage = curPage == null ? 1 : curPage;
        // 每页记录数，从哪开始
        int limit = 8;
        int offset = (curPage - 1) * limit;
        // 获得总记录数，总页数
        int allCount = answerMapper.selectAnswerCountByUserId(userId);
        int allPage = 0;
        if (allCount <= limit) {
            allPage = 1;
        } else if (allCount / limit == 0) {
            allPage = allCount / limit;
        } else {
            allPage = allCount / limit + 1;
        }

        // 构造查询map
        Map<String, Object> map = new HashMap<>();
        map.put("offset", offset);
        map.put("limit", limit);
        map.put("userId", userId);
        // 得到某页数据列表
        List<Answer> answerList = answerMapper.listAnswerByUserId(map);

        //获取答案的被点赞的次数
        Jedis jedis = jedisPool.getResource();
        for (Answer answer : answerList){
            Long likedCount = jedis.zcard(answer.getAnswerId() + RedisKey.LIKED_ANSWER);
            answer.setLikedCount(Integer.parseInt(likedCount+""));
        }

        PageBean<Answer> pageBean = new PageBean<>(allPage,curPage);
        pageBean.setList(answerList);

        return pageBean;
    }

    //点赞答案
    public void likeAnswer(Integer userId,Integer answerId){
        //更新答案被点赞数量
        answerMapper.updateLikedCount(answerId);
        //更新用户的被点赞数量
        userMapper.updateLikedCountByAnswerId(answerId);
        Jedis jedis = jedisPool.getResource();
        jedis.zadd(userId + RedisKey.LIKE_ANSWER, new Date().getTime(), String.valueOf(answerId));
        jedis.zadd(answerId + RedisKey.LIKED_ANSWER, new Date().getTime(), String.valueOf(userId));

        jedisPool.returnResource(jedis);

        //插入一条点赞的消息
        Message message = new Message();
        message.setType(Message.TYPE_LIKED);
        message.setSecondType(1);

        Date date = new Date();
        message.setMessageDate(MyUtil.formatDate(date));
        message.setMessageTime(date.getTime());
        message.setFromUserId(userId);
        message.setFromUserName(userMapper.selectUsernameByUserId(userId));

        Question question = questionMapper.selectQuestionByAnswerId(answerId);
        message.setQuestionId(question.getQuestionId());
        message.setQuestionTitle(question.getQuestionTitle());
        message.setAnswerId(answerId);
        message.setUserId(answerMapper.selectUserIdByAnswerId(answerId));
        messageMapper.insertTypeLiked(message);

    }

    public Map<String, Object> listTodayHotAnswer() {
        Map<String, Object> map = new HashMap<>();
        long period = 1000 * 60 * 60 * 24L;
        long today = new Date().getTime();
        System.out.println("period:" + period);
        System.out.println("today:" + today);
        System.out.println("today - period:" + (today - period));
        System.out.println(new Date(today - period));
        List<Answer> answerList = answerMapper.listAnswerByCreateTime(today - period);
        map.put("answerList", answerList);
        return map;
    }

    public Map<String, Object> listMonthHotAnswer() {
        Map<String, Object> map = new HashMap<>();
        long period = 1000 * 60 * 60 * 24 * 30L;
        long today = new Date().getTime();
        System.out.println("period:" + period);
        System.out.println("month:" + today);
        System.out.println("month - period:" + (today - period));
        System.out.println(new Date(today - period));
        List<Answer> answerList = answerMapper.listAnswerByCreateTime(today - period);
        map.put("answerList", answerList);
        return map;
    }


}
