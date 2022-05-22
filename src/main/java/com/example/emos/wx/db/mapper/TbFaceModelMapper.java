package com.example.emos.wx.db.mapper;

import com.example.emos.wx.db.pojo.TbFaceModel;
import org.apache.ibatis.annotations.Mapper;

/**
* @author 20773
* @description 针对表【tb_face_model】的数据库操作Mapper
* @createDate 2022-02-09 21:47:10
* @Entity com.example.emos.wx.db.pojo.TbFaceModel
*/
@Mapper
public interface TbFaceModelMapper {
    public String searchFaceModel(int userId);

    public void insert(TbFaceModel faceModel);

    public int deleteFaceModel(int userId);

}
