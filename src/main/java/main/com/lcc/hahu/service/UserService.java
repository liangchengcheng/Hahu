package main.com.lcc.hahu.service;

import freemarker.ext.beans.HashAdapter;
import main.com.lcc.hahu.async.MailTask;
import main.com.lcc.hahu.mapper.AnswerMapper;
import main.com.lcc.hahu.mapper.CommentMapper;
import main.com.lcc.hahu.mapper.MessageMapper;
import main.com.lcc.hahu.mapper.UserMapper;
import main.com.lcc.hahu.model.Answer;
import main.com.lcc.hahu.model.Message;
import main.com.lcc.hahu.model.User;
import main.com.lcc.hahu.util.MyConstant;
import main.com.lcc.hahu.util.MyUtil;
import main.com.lcc.hahu.util.RedisKey;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by liangchengcheng on 2017/7/4.
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AnswerMapper answerMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private JedisPool jedisPool;

    public Map<String, String> register(String username, String email, String password) {
        Map<String, String> map = new HashMap<>();
        //进行邮箱的基本校验
        Pattern p = Pattern.compile("^([a-zA-Z0-9_-])+@([a-zA-Z0-9_-])+((\\.[a-zA-Z0-9_-]{2,3}){1,2})$");
        Matcher m = p.matcher(email);
        if (!m.matches()) {
            map.put("regi-email-error", "请输入正确的邮箱");
            return map;
        }
        //进行用户名长度的基本校验
        if (StringUtils.isEmpty(username) || username.length() > 10) {
            map.put("regi-username-error", "用户名长度须在1-10个字符");
            return map;
        }
        //校验密码长度
        p = Pattern.compile("^\\w{6,20}$");
        m = p.matcher(password);
        if (!m.matches()) {
            map.put("regi-password-error", "密码长度须在6-20个字符");
            return map;
        }
        //检测邮箱是否被注册
        int emailCount = userMapper.selectEmailCount(email);
        if (emailCount > 0) {
            map.put("regi-email-error", "该邮箱已注册");
            return map;
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(MyUtil.md5(password));
        //构建user,设置成没有激活的状态
        String activeCode = MyUtil.createRandomCode();
        user.setActivationCode(activeCode);
        user.setJoinTime(new Date().getTime());

        user.setUsername(username);
        user.setAvatarUrl(MyConstant.QINIU_IMAGE_URL + "head.jpg");
        //发送邮件
        taskExecutor.execute(new MailTask(activeCode, user.getEmail(), javaMailSender, 1));
        //向数据库插入记录
        userMapper.insertUser(user);

        //设置默认关注用户
        Jedis jedis = jedisPool.getResource();
        jedis.zadd(user.getUserId() + RedisKey.FOLLOW_PEOPLE, new Date().getTime(), String.valueOf(3));
        jedis.zadd(3 + RedisKey.FOLLOWED_PEOPLE, new Date().getTime(), String.valueOf(user.getUserId()));
        jedis.zadd(user.getUserId() + RedisKey.FOLLOW_PEOPLE, new Date().getTime(), String.valueOf(4));
        jedis.zadd(4 + RedisKey.FOLLOWED_PEOPLE, new Date().getTime(), String.valueOf(user.getUserId()));

        jedisPool.returnResource(jedis);
        map.put("ok", "注册完成");
        return map;
    }

    /**
     * 登录
     */
    public Map<String, Object> login(String email, String password, HttpServletResponse response) {
        Map<String, Object> map = new HashMap<>();
        //校验用户名和密码是否正确
        Integer userId = userMapper.selectUserIdByEmailAndPassword(email, MyUtil.md5(password));
        if (userId == null) {
            map.put("error", "用户名或者密码错误");
            return map;
        }
        //校验用户账号是否被激活了
        Integer activationState = userMapper.selectActivationStateByUserId(userId);
        if (activationState != 1) {
            map.put("error", "您的账号还没有激活");
            return map;
        }
        //设置登录的cookie
        String loginToken = MyUtil.createRandomCode();
        Cookie cookie = new Cookie("loginToken", loginToken);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 30);
        response.addCookie(cookie);

        //将token:userId存入到redis,并设置过期时间
        Jedis jedis = jedisPool.getResource();
        jedis.set(loginToken, userId.toString(), "NX", "EX", 60 * 60 * 24 * 30);
        jedisPool.returnResource(jedis);

        // 将用户信息返回，存入localStorage
        User user = userMapper.selectUserInfoByUserId(userId);
        user.setUserId(userId);
        map.put("userInfo", user);

        return map;
    }

    /**
     * 激活用户
     */
    public void activate(String activationCode) {
        userMapper.updateActivationStateByActivationCode(activationCode);
    }

    /**
     * 从redis里面获取userId
     */
    public Integer getUserIdFromRedis(HttpServletRequest request) {
        String loginToken = null;
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("loginToken")) {
                loginToken = cookie.getValue();
                break;
            }
        }

        //这里应该判断一下loginToken是否为空
        Jedis jedis = jedisPool.getResource();
        String userId = jedis.get(loginToken);
        return Integer.parseInt(userId);
    }

    /**
     * 退出登录
     */
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        String loginToken = null;
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("loginToken")) {
                loginToken = cookie.getValue();
                //从缓存中清除loginToken
                Jedis jedis = jedisPool.getResource();
                jedis.del(loginToken);
                jedisPool.returnResource(jedis);
                break;
            }
        }

        Cookie cookie = new Cookie("loginToken", "");
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 30);
        response.addCookie(cookie);
        return loginToken;
    }

    /**
     * 获取人的基本的属性。
     * 梁铖城 粉丝人数和阅读人数一般存在redis
     */
    public Map<String, Object> profile(Integer userId, Integer localUserId) {
        Map<String, Object> map = new HashMap<>();
        User user = userMapper.selectProfileInfoByUserId(userId);
        if (userId.equals(localUserId)) {
            map.put("isSelf", true);
        } else {
            map.put("isSelf", false);
        }

        Jedis jedis = jedisPool.getResource();
        //Redis Zcard 命令用于计算集合中元素的数量。
        Long followCount = jedis.zcard(userId + RedisKey.FOLLOW_PEOPLE);
        Long followedCount = jedis.zcard(userId + RedisKey.FOLLOWED_PEOPLE);
        Long followTopicCount = jedis.zcard(userId + RedisKey.FOLLOW_TOPIC);
        Long followQuestionCount = jedis.zcard(userId + RedisKey.FOLLOW_QUESTION);
        Long followCollectionCount = jedis.zcard(userId + RedisKey.FOLLOW_COLLECTION);

        user.setFollowCount(Integer.parseInt(followCount + ""));
        user.setFollowedCount(Integer.parseInt(followedCount + ""));
        user.setFollowTopicCount(Integer.parseInt(followTopicCount + ""));
        user.setFollowQuestionCount(Integer.parseInt(followQuestionCount + ""));
        user.setFollowCollectionCount(Integer.parseInt(followCollectionCount + ""));

        map.put("user", user);
        return map;
    }

    public boolean judgePeopleFollowUser(Integer localUserId, Integer userId) {
        Jedis jedis = jedisPool.getResource();
        Long rank = jedis.zrank(localUserId + RedisKey.FOLLOW_PEOPLE, String.valueOf(userId));
        jedisPool.returnResource(jedis);
        return rank == null ? false : true;
    }

    /**
     * 喜欢一个用户
     */
    public void followUser(Integer localUserId, Integer userId) {
        Jedis jedis = jedisPool.getResource();
        jedis.zadd(localUserId + RedisKey.FOLLOW_PEOPLE, new Date().getTime(), String.valueOf(userId));
        jedis.zadd(userId + RedisKey.FOLLOWED_PEOPLE, new Date().getTime(), String.valueOf(localUserId));
        jedisPool.returnResource(jedis);

        //插入数据库一条关注信息
        Message message = new Message();
        message.setType(Message.TYPE_FOLLOWED);
        message.setSecondType(1);
        Date date = new Date();

        message.setMessageDate(MyUtil.formatDate(date));
        message.setMessageTime(date.getTime());
        message.setFromUserId(localUserId);
        message.setFromUserName(userMapper.selectUsernameByUserId(localUserId));
        ;
        message.setUserId(userId);
        messageMapper.insertTypeFollowed(message);
    }

    public void unfollowUser(Integer localUserId, Integer userId) {
        Jedis jedis = jedisPool.getResource();
        jedis.zrem(localUserId + RedisKey.FOLLOW_PEOPLE, String.valueOf(userId));
        jedis.zrem(userId + RedisKey.FOLLOWED_PEOPLE, String.valueOf(localUserId));
        jedisPool.returnResource(jedis);

    }

    /**
     * 获取所有喜欢你的人的集合
     */
    public List<User> listFollowerUser(Integer userId) {
        Jedis jedis = jedisPool.getResource();
        //获取所有关注用户的id
        Set<String> idSet = jedis.zrange(userId + RedisKey.FOLLOWED_PEOPLE, 0, -1);
        List<Integer> idList = MyUtil.StringSetToIntegerList(idSet);

        List<User> list = new ArrayList<>();
        if (idList.size() > 0) {
            list = userMapper.listUserInfoByUserId(idList);
        }

        jedisPool.returnResource(jedis);
        return list;
    }

    public Map<String, Object> getIndexDetail(Integer userId, Integer curPage) {
        Map<String, Object> map = new HashMap<>();
        Jedis jedis = jedisPool.getResource();

        Set<String> idSet = jedis.zrange(userId + RedisKey.FOLLOW_PEOPLE, 0, -1);
        List<Integer> idList = MyUtil.StringSetToIntegerList(idSet);
        List<Answer> answerList = new ArrayList<Answer>();

        if (idList.size() > 0) {
            answerList = _getIndexDetail(idList, curPage);
            for (Answer answer : answerList) {
                //获取用户的点赞状态
                Long rank = jedis.zrank(answer.getAnswerId() + RedisKey.LIKE_ANSWER, String.valueOf(userId));
                System.out.println("rank:" + rank);
                answer.setLikeState(rank == null ? "false" : "true");
            }
        }

        map.put("answerList", answerList);
        jedisPool.returnResource(jedis);
        return map;
    }

    /**
     * 获取回答,评论数
     */
    private List<Answer> _getIndexDetail(List<Integer> idList, Integer curPage) {
        //当请求的页数是空的时候
        curPage = curPage == null ? 1 : curPage;
        //每页记录数,从哪开始
        int limit = 8;
        int offset = (curPage - 1) * limit;
        //构建一个map
        Map<String, Object> map = new HashMap<>();
        map.put("offset", offset);
        map.put("limit", limit);
        map.put("userIdList", idList);

        List<Answer> answerList = answerMapper.listAnswerByUserIdList(map);

        for (Answer answer : answerList) {
            int commentCount = commentMapper.selectAnswerCommentCountByAnswerId(answer.getAnswerId());
            answer.setCommentCount(commentCount);
        }

        return answerList;
    }

    public User getProfileInfo(Integer userId) {
        User user = userMapper.selectProfileInfoByUserId(userId);
        return user;
    }

    public void updateProfile(User user) {
        userMapper.updateProfile(user);
    }

    public Map<String, String> updatePassword(Integer userId, String password, String newpassword) {

        Map<String, String> map = new HashMap<String, String>();
        int userCount = userMapper.selectUserCountByUserIdAndPassword(userId, MyUtil.md5(password));
        if (userCount < 1) {
            map.put("error", "原密码不正确");
            return map;
        }
        userMapper.updatePassword(userId, MyUtil.md5(newpassword));
        return map;
    }

    public void updateAvatarUrl(Integer userId, String avatarUrl) {
        userMapper.updateAvatarUrl(userId, avatarUrl);

    }
}
