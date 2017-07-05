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
}
