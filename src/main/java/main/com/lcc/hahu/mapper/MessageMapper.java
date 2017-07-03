package main.com.lcc.hahu.mapper;

import java.util.List;
import main.com.lcc.hahu.model.Message;
import org.apache.ibatis.annotations.Param;


public interface MessageMapper {

    void insertTypeFollowed(Message message);

    void insertTypeLiked(Message message);

    void insertTypeComment(Message message);

    void insertTypeAnswer(Message message);

    List<Message> listMessageByUserId(@Param("userId") Integer userId);

}
