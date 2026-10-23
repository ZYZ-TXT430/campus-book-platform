package com.campusbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusbook.entity.User;

/**
 * 用户 Mapper。
 *
 * <p>继承 BaseMapper&lt;User&gt; 后自动拥有这些方法，简单的单表 CRUD 一行 SQL 都不用写：
 * insert、deleteById、updateById、selectById、selectList、selectCount、selectPage……</p>
 *
 * <p>复杂 SQL 仍然可以写 UserMapper.xml，与 MP 共存；
 * 这个模块暂时没有复杂查询，所以删掉了原来的 XML。</p>
 */
public interface UserMapper extends BaseMapper<User> {
}
