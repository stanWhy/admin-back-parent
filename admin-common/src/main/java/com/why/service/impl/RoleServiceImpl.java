package com.why.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.why.common.annotation.DataScope;
import com.why.common.exception.admin.AdminException;
import com.why.common.utils.StringUtils;
import com.why.common.utils.TypeUtils;
import com.why.framework.shiro.ShiroUtils;
import com.why.mapper.RoleMapper;
import com.why.model.Role;
import com.why.model.RoleDept;
import com.why.model.RoleMenu;
import com.why.model.UserRole;
import com.why.service.RoleDeptService;
import com.why.service.RoleMenuService;
import com.why.service.RoleService;
import com.why.service.UserRoleService;
import com.why.service.base.impl.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description:
 * @author: wanghongyu | stan.wang@paytm.com
 * @create: 2021/6/25
 **/
@Service
@Slf4j
public class RoleServiceImpl extends BaseServiceImpl<RoleMapper, Role> implements RoleService {
    @Autowired
    private RoleMenuService roleMenuService;
    @Autowired
    private RoleDeptService roleDeptService;
    @Autowired
    private UserRoleService userRoleService;

    @Override
    @DataScope
    public List<Role> selectRoleList(Role role) {
        return baseMapper.selectRoleList(role);
    }

    @Override
    public Set<String> selectRoleKeys(Long userId) {
        List<Role> perms = baseMapper.selectRolesByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (Role perm : perms) {
            if (StringUtils.isNotNull(perm)) {
                permsSet.addAll(Arrays.asList(perm.getRoleKey().trim().split(",")));
            }
        }
        return permsSet;
    }

    @Override
    public List<Role> selectRolesByUserId(Long userId) {
        List<Role> userRoles = baseMapper.selectRolesByUserId(userId);
        List<Role> roles = selectRoleAll();
        for (Role role : roles) {
            for (Role userRole : userRoles) {
                if (role.getRoleId().longValue() == userRole.getRoleId().longValue()) {
                    role.setFlag(true);
                    break;
                }
            }
        }
        return roles;
    }

    @Override
    public List<Role> selectRoleAll() {
        return ((RoleService) AopContext.currentProxy()).selectRoleList(new Role());
    }

    @Override
    public boolean deleteRoleById(Long roleId) {
        return delete().eq(Role::getRoleId, roleId).execute();
    }

    @Override
    public boolean deleteRoleByIds(String ids) {
        List<Long> roleIds = StringUtils.split2List(ids, TypeUtils::castToLong);
        for (Long roleId : roleIds) {
            Role role = getById(roleId);
            if (userRoleService.query().eq(UserRole::getRoleId, roleId).exist()) {
                throw new AdminException(HttpServletResponse.SC_BAD_REQUEST, role.getRoleName() + "????????????????????????");
            }
        }
        return delete().in(Role::getRoleId, roleIds).execute();
    }

    @Override
    public boolean insertRole(Role role) {
        // ??????????????????
        save(role);
        ShiroUtils.clearCachedAuthorizationInfo();
        return insertRoleMenu(role);
    }

    @Override
    public boolean updateRole(Role role) {
        // ??????????????????
        updateById(role);
        ShiroUtils.clearCachedAuthorizationInfo();
        // ???????????????????????????
        roleMenuService.delete().eq(RoleMenu::getRoleId, role.getRoleId()).execute();
        return insertRoleMenu(role);
    }

    @Override
    public boolean authDataScope(Role role) {
        // ??????????????????
        updateById(role);
        // ???????????????????????????
        roleDeptService.delete().eq(RoleDept::getRoleId, role.getRoleId()).execute();
        // ?????????????????????????????????????????????
        return insertRoleDept(role);
    }

    /**
     * ????????????????????????
     *
     * @param role ????????????
     */
    public boolean insertRoleMenu(Role role) {
        roleMenuService.saveBatch(
                Arrays.stream(role.getMenuIds()).map(menuId -> {
                    RoleMenu rm = new RoleMenu();
                    rm.setRoleId(role.getRoleId());
                    rm.setMenuId(menuId);
                    return rm;
                }).collect(Collectors.toList())
        );
        return true;
    }

    /**
     * ????????????????????????(????????????)
     *
     * @param role ????????????
     */
    private boolean insertRoleDept(Role role) {
        roleDeptService.saveBatch(
                Arrays.stream(role.getDeptIds()).map(deptId -> {
                    RoleDept rd = new RoleDept();
                    rd.setRoleId(role.getRoleId());
                    rd.setDeptId(deptId);
                    return rd;
                }).collect(Collectors.toList())
        );
        return true;
    }

    @Override
    public boolean checkRoleNameUnique(Role role) {
        Long roleId = role.getRoleId();
        Role info = query().eq(Role::getRoleName, role.getRoleName()).getOne();
        return Objects.isNull(info) || info.getRoleId().equals(roleId);
    }

    @Override
    public boolean checkRoleKeyUnique(Role role) {
        Long roleId = role.getRoleId();
        Role info = query().eq(Role::getRoleKey, role.getRoleKey()).getOne();
        return Objects.isNull(info) || info.getRoleId().equals(roleId);
    }

    @Override
    public boolean changeStatus(Role role) {
        Role updateRole = new Role();
        updateRole.setStatus(role.getStatus());
        return update(updateRole, Wrappers.<Role>lambdaQuery().eq(Role::getRoleId, role.getRoleId()));

    }

    @Override
    public boolean deleteAuthUser(UserRole userRole) {
        return userRoleService.delete().eq(UserRole::getRoleId, userRole.getRoleId()).eq(UserRole::getUserId, userRole.getUserId()).execute();
    }

    /**
     * ??????????????????????????????
     *
     * @param roleId  ??????ID
     * @param userIds ???????????????????????????ID
     * @return ??????
     */
    @Override
    public boolean deleteAuthUsers(Long roleId, String userIds) {
        return userRoleService.delete().eq(UserRole::getRoleId, roleId).in(UserRole::getUserId, StringUtils.split2List(userIds)).execute();

    }

    /**
     * ??????????????????????????????
     *
     * @param roleId  ??????ID
     * @param userIds ???????????????????????????ID
     * @return ??????
     */
    @Override
    public boolean insertAuthUsers(Long roleId, String userIds) {
        userRoleService.saveBatch(
                StringUtils.split2List(userIds, TypeUtils::castToLong).stream().map(userId -> {
                    UserRole ur = new UserRole();
                    ur.setUserId(userId);
                    ur.setRoleId(roleId);
                    return ur;
                }).collect(Collectors.toList())
        );
        return true;
    }
}
