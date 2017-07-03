package main.com.lcc.hahu.mapper;

import main.com.lcc.hahu.model.Collection;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Created by liangchengcheng on 2017/7/4.
 */
public interface CollectionMapper {

    void insertCollection(Collection collection);

    List<Collection> listCreatingCollectionByUserId(@Param("userId") Integer userId);

    Collection selectCollectionByCollectionId(@Param("collectionId") Integer collectionId);

    Integer selectUserIdByCollectionId(@Param("collectionId") Integer collectionId);

    List<Collection> listCollectionByCollectionId(List<Integer> idList);

}

