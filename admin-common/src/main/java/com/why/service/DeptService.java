package com.why.service;

import com.why.model.Dept;
import com.why.model.Role;
import com.why.model.base.Ztree;
import com.why.service.base.BaseService;

import java.util.List;

/**
 * @description:
 * @author: wanghongyu | stan.wang@paytm.com
 * @create: 2021/6/25
 **/
public interface DeptService extends BaseService<Dept> {
    /**
     * 查询部门管理数据
     *
     * @param dept 部门信息
     * @return 部门信息集合
     */
    List<Dept> selectDeptList(Dept dept);

    /**
     * 查询部门管理树
     *
     * @param dept 部门信息
     * @return 所有部门信息
     */
    List<Ztree> selectDeptTree(Dept dept);

    /**
     * 根据角色ID查询菜单
     *
     * @param role 角色对象
     * @return 菜单列表
     */
    List<Ztree> roleDeptTreeData(Role role);

    /**
     * 查询部门是否存在用户
     *
     * @param deptId 部门ID
     * @return 结果 true 存在 false 不存在
     */
    boolean checkDeptExistUser(Long deptId);

    /**
     * 新增保存部门信息
     *
     * @param dept 部门信息
     * @return 结果
     */
    boolean insertDept(Dept dept);

    /**
     * 修改保存部门信息
     *
     * @param dept 部门信息
     * @return 结果
     */
    boolean updateDept(Dept dept);

    /**
     * 根据部门ID查询信息
     *
     * @param deptId 部门ID
     * @return 部门信息
     */
    Dept selectDeptById(Long deptId);

    /**
     * 校验部门名称是否唯一
     *
     * @param dept 部门信息
     * @return 结果
     */
    boolean checkDeptNameUnique(Dept dept);
}
