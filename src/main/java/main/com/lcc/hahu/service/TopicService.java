package main.com.lcc.hahu.service;

import main.com.lcc.hahu.mapper.AnswerMapper;
import main.com.lcc.hahu.mapper.QuestionMapper;
import main.com.lcc.hahu.mapper.TopicMapper;
import main.com.lcc.hahu.model.Answer;
import main.com.lcc.hahu.model.PageBean;
import main.com.lcc.hahu.model.Question;
import main.com.lcc.hahu.model.Topic;
import main.com.lcc.hahu.util.MyUtil;
import main.com.lcc.hahu.util.RedisKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;

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

    public List<Topic> listTopicByParentTopicId(Integer parentTopicId){
        List<Topic> list = topicMapper.listTopicByParentId(parentTopicId);
        return list;
    }

    public Map<String,Object> getTopicDetail(Integer topicId,Boolean allQuestion,Integer curPage, Integer userId){
        Map<String,Object> map = new HashMap<>();
        //获取话题信息
        Topic topic = topicMapper.selectTopicByTopicId(topicId);
        Jedis jedis = jedisPool.getResource();
        //获取被关注的人数
        Long followedCount = jedis.zcard(topicId+ RedisKey.FOLLOWED_TOPIC);
        topic.setFollowedCount(Integer.parseInt(followedCount+""));
        //要是不是请求全部问题列表
        if (allQuestion == null || allQuestion.equals(false)){
            //获取这个话题下答案列表的分页数据
            List<Integer> questionIdList = topicMapper.selectQuestionIdByTopicId(topic.getTopicId());
            if (questionIdList != null && questionIdList.size() > 0){
                PageBean<Answer> pageBean = _listGoodAnswerByQuestionId(questionIdList, curPage, jedis, userId);
                map.put("pageBean", pageBean);
            }else {
                map.put("pageBean", new PageBean<Answer>());
            }
        }else {
            PageBean<Question> pageBean = _listAllQuestionByTopicId(topic.getTopicId(), curPage, jedis);
            map.put("pageBean", pageBean);
            // 告诉页面返回的是问题列表，而不是答案列表
            map.put("allQuestion", true);
        }

        map.put("topic", topic);
        jedisPool.returnResource(jedis);
        return map;
    }

    public boolean judgePeopleFollowTopic(Integer userId, Integer topicId) {
        Jedis jedis = jedisPool.getResource();
        Long rank = jedis.zrank(userId + RedisKey.FOLLOW_TOPIC, String.valueOf(topicId));
        jedisPool.returnResource(jedis);

        return rank == null ? false : true;
    }

    // 关注话题
    public void followTopic(Integer userId, Integer topicId) {
        Jedis jedis = jedisPool.getResource();
        jedis.zadd(userId + RedisKey.FOLLOW_TOPIC, new Date().getTime(), String.valueOf(topicId));
        jedis.zadd(topicId + RedisKey.FOLLOWED_TOPIC, new Date().getTime(), String.valueOf(userId));
        jedisPool.returnResource(jedis);

        // 将话题被关注数量加1
        topicMapper.updateFollowedCount(topicId);
    }

    // 取消关注话题
    public void unfollowTopic(Integer userId, Integer topicId) {
        Jedis jedis = jedisPool.getResource();
        jedis.zrem(userId + RedisKey.FOLLOW_TOPIC, String.valueOf(topicId));
        jedis.zrem(topicId + RedisKey.FOLLOWED_TOPIC, String.valueOf(userId));
        jedisPool.returnResource(jedis);
    }

    private PageBean<Answer> _listGoodAnswerByQuestionId(List<Integer> questionIdList, Integer curPage, Jedis jedis, Integer userId) {
        // 能执行该函数，说明questionIdList不空
        // 当请求页数为空时
        curPage = curPage == null ? 1 : curPage;
        // 每页记录数，从哪开始
        int limit = 8;
        int offset = (curPage - 1) * limit;
        // 获得总记录数，总页数
        int allCount = answerMapper.listAnswerCountByQuestionId(questionIdList);
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
        map.put("questionIdList", questionIdList);
        // 得到某页数据列表
        List<Answer> answerList = answerMapper.listGoodAnswerByQuestionId(map);

        for (Answer answer : answerList) {
            // 获取用户点赞状态
            Long rank = jedis.zrank(answer.getAnswerId() + RedisKey.LIKED_ANSWER, String.valueOf(userId));
            answer.setLikeState(rank == null ? "false" : "true");
        }
        // 构造PageBean
        PageBean<Answer> pageBean = new PageBean<>(allPage, curPage);
        pageBean.setList(answerList);

        return pageBean;
    }

    private PageBean<Question> _listAllQuestionByTopicId(Integer topicId, Integer curPage, Jedis jedis) {
        // 当请求页数为空时
        curPage = curPage == null ? 1 : curPage;
        // 每页记录数，从哪开始
        int limit = 8;
        int offset = (curPage - 1) * limit;
        // 获得总记录数，总页数
        int allCount = questionMapper.selectQuestionCountByTopicId(topicId);
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
        map.put("topicId", topicId);
        // 得到某页数据列表
        List<Integer> questionIdList = questionMapper.listQuestionIdByTopicId(map);
        System.out.println(questionIdList);
        List<Question> questionList = new ArrayList<Question>();
        if (questionIdList.size() > 0) {
            questionList = questionMapper.listQuestionByQuestionId(questionIdList);
            for (Question question : questionList) {
                Long followedCount = jedis.zcard(question.getQuestionId() + RedisKey.FOLLOWED_QUESTION);
                question.setFollowedCount(Integer.parseInt(followedCount + ""));
            }
        }

        // 构造PageBean
        PageBean<Question> pageBean = new PageBean<>(allPage, curPage);
        pageBean.setList(questionList);

        return pageBean;
    }

    //列出所挂住的话题
    public List<Topic> listFollowingTopic(Integer userId){
        Jedis jedis = jedisPool.getResource();
        //获取关注的话题的id的集合
        Set<String> idSet = jedis.zrange(userId + RedisKey.FOLLOWED_TOPIC,0,-1);
        List<Integer> idList = MyUtil.StringSetToIntegerList(idSet);

        List<Topic> list = new ArrayList<>();
        if (idList!= null && idList.size()>0){
            list = topicMapper.listTopicByTopicId(idList);
        }

        jedisPool.returnResource(jedis);
        return list;
    }

    /**
     * 根据话题的关键字搜索话题
     */
    public Map<String,Object> listTopicByTopicName(String topicName){
        Map<String,Object> map = new HashMap<>();
        List<Topic> topicList = topicMapper.listTopicByTopicName("%" + topicName + "%");
        map.put("topList",topicList);
        return map;
    }

}
