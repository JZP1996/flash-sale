package com.jzp.mapper;

import com.jzp.dataobject.UserPassword;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPasswordMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table user_password
     *
     * @mbg.generated Sat Jun 06 20:52:47 CST 2020
     */
    int insert(UserPassword record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table user_password
     *
     * @mbg.generated Sat Jun 06 20:52:47 CST 2020
     */
    int insertSelective(UserPassword record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table user_password
     *
     * @mbg.generated Sat Jun 06 20:52:47 CST 2020
     */
    UserPassword selectByPrimaryKey(Integer id);

    UserPassword selectByUserId(Integer userId);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table user_password
     *
     * @mbg.generated Sat Jun 06 20:52:47 CST 2020
     */
    int updateByPrimaryKeySelective(UserPassword record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table user_password
     *
     * @mbg.generated Sat Jun 06 20:52:47 CST 2020
     */
    int updateByPrimaryKey(UserPassword record);
}