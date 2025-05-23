package com.ywlabs.springai.mapper;

import com.ywlabs.springai.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface EmployeeMapper {
    
    @Select("SELECT * FROM employees ORDER BY id")
    List<Employee> findAll();
    
    @Select("SELECT * FROM employees WHERE id = #{id}")
    Employee findById(Long id);
} 