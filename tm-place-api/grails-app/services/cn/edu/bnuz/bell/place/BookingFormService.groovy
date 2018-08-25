package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.ForbiddenException
import cn.edu.bnuz.bell.http.NotFoundException
import cn.edu.bnuz.bell.master.Term
import cn.edu.bnuz.bell.organization.Department
import cn.edu.bnuz.bell.organization.Student
import cn.edu.bnuz.bell.security.SecurityService
import cn.edu.bnuz.bell.security.User
import cn.edu.bnuz.bell.security.UserType
import cn.edu.bnuz.bell.master.TermService
import cn.edu.bnuz.bell.system.SystemConfigService
import cn.edu.bnuz.bell.workflow.DomainStateMachineHandler
import cn.edu.bnuz.bell.workflow.commands.SubmitCommand
import grails.gorm.transactions.Transactional
import org.hibernate.SessionFactory
import org.hibernate.result.ResultSetOutput

import javax.persistence.ParameterMode
import java.time.LocalDate

@Transactional
class BookingFormService {
    SessionFactory sessionFactory
    TermService termService
    SecurityService securityService
    DomainStateMachineHandler domainStateMachineHandler
    SystemConfigService systemConfigService

    /**
     * 获取指定用户ID的申请单用于显示
     * @param userId 用户ID
     * @return 申请单列表
     */
    def list(String userId, Integer offset, Integer max) {
        BookingForm.executeQuery '''
select new map(
  bf.id as id,
  bd.name as department,
  bt.name as type,
  bf.reason as reason,
  bf.dateModified as dateModified,
  bf.status as status
)
from BookingForm bf
join bf.department bd
join bf.type bt
where bf.user.id = :userId
order by bf.dateModified desc
''', [userId: userId], [offset: offset, max: max]
    }

    def listCount(String userId) {
        BookingForm.countByUser(User.load(userId))
    }

    def getNotice() {
        systemConfigService.get(BookingForm.CONFIG_NOTICE, '')
    }

    Map getFormInfo(Long id) {
        def results = BookingForm.executeQuery '''
select new map(
  form.id as id,
  term.id as term,
  user.id as userId,
  user.name as userName,
  user.userType as userType,
  user.longPhone as phoneNumber,
  department.id as departmentId,
  department.name as departmentName,
  type.id as bookingTypeId,
  type.name as bookingTypeName,
  form.reason as reason,
  form.dateCreated as dateCreated,
  form.dateModified as dateModified,
  form.dateSubmitted as dateSubmitted,
  checker.name as checker,
  form.dateChecked as dateChecked,
  approver.name as approver,
  form.dateApproved as dateApproved,
  form.status as status,
  form.workflowInstance.id as workflowInstanceId
)
from BookingForm form
join form.user user
join form.department department
join form.type type
join form.term term
left join form.checker checker
left join form.approver approver
where form.id = :id
''', [id: id]

        if (!results) {
            return null
        }

        def form = results[0]
        def items = BookingItem.executeQuery '''
select new map(
  item.id as id,
  place.id as placeId,
  place.name as placeName,
  place.seat as placeSeat,
  place.type as placeType,
  item.startWeek as startWeek,
  item.endWeek as endWeek,
  item.oddEven as oddEven,
  item.dayOfWeek as dayOfWeek,
  section.id as sectionId,
  section.name as sectionName,
  section.includes as sectionIncludes,
  item.occupied as occupied
)
from BookingItem item
join item.place place
join item.section section
where item.form.id = :formId
''', [formId: id]
        form.items = items.collect {
            [
                    id       : it.id,
                    place    : [
                            id  : it.placeId,
                            name: it.placeName,
                            seat: it.placeSeat,
                            type: it.placeType,
                    ],
                    startWeek: it.startWeek,
                    endWeek  : it.endWeek,
                    oddEven  : it.oddEven,
                    dayOfWeek: it.dayOfWeek,
                    section  : [
                            id      : it.sectionId,
                            name    : it.sectionName,
                            includes: it.sectionIncludes,
                    ],
                    occupied : it.occupied,
            ]
        }
        return form
    }

    /**
     * 获取申请单信息，用于显示
     * @param id 申请ID
     * @return 申请信息
     */
    def getFormForShow(String userId, Long id) {
        def form = getFormInfo(id)

        if (!form) {
            throw new NotFoundException()
        }

        if (form.userId != userId) {
            throw new ForbiddenException()
        }

        // 防止跨学期修改提交
        form.editable = form.term == termService.activeTerm.id &&
                domainStateMachineHandler.canUpdate(form)

        return form
    }

    def getFormForCreate(String userId) {
        def user = User.get(userId)
        def term = termService.activeTerm
        def departments = getBookingDepartments(user.departmentId)
        def department = departments.find { it.id == user.departmentId }
        def departmentId = department ? department.id : departments[0].id
        def bookingTypes = getBookingTypes(departmentId)

        return [
                term        : [
                        startWeek  : term.startWeek,
                        maxWeek    : term.maxWeek,
                        currentWeek: term.currentWorkWeek,
                        startDate  : term.startDate,
                        swapDates  : term.swapDates,
                ],
                departments : departments,
                bookingTypes: bookingTypes,
                bookingDays : getBookingDays(user.userType),
                placeTypes  : getPlaceTypes(user.userType),
                sections    : BookingSection.findAll(),
                form        : [
                        userId       : user.id,
                        userName     : user.name,
                        phoneNumber  : user.longPhone,
                        departmentId : departmentId,
                        bookingTypeId: bookingTypes[0].id,
                        items        : [],
                ],
                today       : LocalDate.now(),
        ]
    }

    def getFormForEdit(String userId, Long id) {
        def form = getFormInfo(id)

        if (form.userId != userId) {
            throw new ForbiddenException()
        }

        if (!domainStateMachineHandler.canUpdate(form)) {
            throw new BadRequestException()
        }

        if (form.term != termService.activeTerm.id) {
            throw new BadRequestException()
        }

        def user = User.get(userId)
        def term = termService.activeTerm
        def departments = getBookingDepartments(user.departmentId)
        def bookingTypes = getBookingTypes(form.departmentId)
        return [
                term        : [
                        startWeek  : term.startWeek,
                        maxWeek    : term.maxWeek,
                        currentWeek: term.currentWorkWeek,
                        startDate  : term.startDate,
                        swapDates  : term.swapDates,
                ],
                departments : departments,
                bookingTypes: bookingTypes,
                bookingDays : getBookingDays(user.userType),
                placeTypes  : getPlaceTypes(user.userType),
                sections    : BookingSection.findAll(),
                form        : form
        ]
    }

    private List<Map<String, String>> getBookingDepartments(String departmentId) {
        BookingAuth.executeQuery '''
select distinct new Map(dept.id as id, dept.name as name)
from BookingAuth bc
join bc.department dept
where dept.id = :departmentId or dept.isTeaching = false or :advancedUser = true
order by dept.id
''', [departmentId: departmentId, advancedUser: advancedUser]
    }

    /**
     * 根据部门查询申请类型
     * @param departmentId 部门ID
     * @return 申请类型列表
     */
    def getBookingTypes(String departmentId) {
        BookingType.executeQuery '''
select new map(bt.id as id, bt.name as name, checker.name as checker)
from BookingAuth bc
join bc.type bt
join bc.department dept
join bc.checker checker
where dept.id = :departmentId
order by checker, name
''', [departmentId: departmentId]
    }

    /**
     * 根据用户类别获取可借用天数
     * @param userType 用户类别
     * @return 可借用天数
     */
    private getBookingDays(UserType userType) {
        if (advancedUser) {
            return -1
        }

        if (userType == UserType.TEACHER) {
            return 0
        } else {
            return 14
        }
    }

    /**
     * 获取指定用户类型的借用类型
     * @param userType 用户类型
     * @return 借用类型列表
     */
    def getPlaceTypes(UserType userType) {
        // TODO: 考虑允许学期和部门
        if (advancedUser) {
            PlaceUserType.executeQuery '''
select distinct place.type
from PlaceUserType put
join put.place place
order by place.type'''
        } else {
            PlaceUserType.executeQuery '''
select distinct place.type
from PlaceUserType put
join put.place place
where put.userType = :userType
order by place.type''', [userType: userType]
        }
    }

    private boolean isAdvancedUser() {
        securityService.hasRole 'ROLE_BOOKING_ADV_USER'
    }

    /**
     * 创建申请单
     * @param userId
     * @param userName
     * @param cmd
     * @return
     */
    BookingForm create(String userId, BookingFormCommand cmd) {
        def now = new Date()

        BookingForm form = new BookingForm(
                user: User.load(userId),
                term: termService.activeTerm,
                department: Department.load(cmd.departmentId),
                type: BookingType.load(cmd.bookingTypeId),
                reason: cmd.reason,
                dateCreated: now,
                dateModified: now,
                status: domainStateMachineHandler.initialState
        )

        cmd.addedItems.each { item ->
            form.addToItems(new BookingItem(
                    place: Place.load(item.placeId),
                    startWeek: item.startWeek,
                    endWeek: item.endWeek,
                    oddEven: item.oddEven,
                    dayOfWeek: item.dayOfWeek,
                    section: BookingSection.load(item.sectionId),
                    occupied: false,
            ))
        }

        form.save()

        domainStateMachineHandler.create(form, userId)

        return form
    }

    BookingForm update(String userId, BookingFormCommand cmd) {
        BookingForm form = BookingForm.get(cmd.id)

        if (!form) {
            throw new NotFoundException()
        }

        if (form.user.id != userId) {
            throw new ForbiddenException()
        }

        if (!domainStateMachineHandler.canUpdate(form)) {
            throw new BadRequestException()
        }

        if (form.term.id != termService.activeTerm.id) {
            throw new BadRequestException()
        }

        form.department = Department.load(cmd.departmentId)
        form.type = BookingType.load(cmd.bookingTypeId)
        form.reason = cmd.reason
        form.dateModified = new Date()

        cmd.addedItems.each { item ->
            form.addToItems(new BookingItem(
                    place: Place.load(item.placeId),
                    startWeek: item.startWeek,
                    endWeek: item.endWeek,
                    oddEven: item.oddEven,
                    dayOfWeek: item.dayOfWeek,
                    section: BookingSection.load(item.sectionId),
                    occupied: false,
            ))
        }

        cmd.removedItems.each {
            def bookingItem = BookingItem.load(it)
            form.removeFromItems(bookingItem)
            bookingItem.delete()
        }

        domainStateMachineHandler.update(form, userId)

        form.save()
    }

    void delete(String userId, Long id) {
        BookingForm form = BookingForm.get(id)

        if (!form) {
            throw new NotFoundException()
        }

        if (form.user.id != userId) {
            throw new ForbiddenException()
        }

        if (!domainStateMachineHandler.canUpdate(form)) {
            throw new BadRequestException()
        }

        if (form.workflowInstance) {
            form.workflowInstance.delete()
        }

        form.delete()
    }

    def submit(String userId, SubmitCommand cmd) {
        BookingForm form = BookingForm.get(cmd.id)

        if (!form) {
            throw new NotFoundException()
        }

        if (form.user.id != userId) {
            throw new ForbiddenException()
        }

        if (!domainStateMachineHandler.canSubmit(form)) {
            throw new BadRequestException()
        }

        domainStateMachineHandler.submit(form, userId, cmd.to, cmd.comment, cmd.title)

        form.dateSubmitted = new Date()
        form.save()
    }

    /**
     * 查找场地
     * @param userType 用户类型
     * @param cmd 查询参数
     * @return [id: 场地ID, name: 名称, seat: 座位数, booking: 正在预订的数量]*
     */
    def findPlaces(UserType userType, FindPlaceCommand cmd) {
        Term term = termService.activeTerm
        def session = sessionFactory.currentSession
        def query = session.createStoredProcedureCall('sp_find_available_place')
        def outputs = query.with {
            [
                    p_term_id    : term.id,
                    p_start_week : cmd.startWeek,
                    p_end_week   : cmd.endWeek,
                    p_even_odd   : cmd.evenOdd,
                    p_day_of_week: cmd.dayOfWeek,
                    p_section_id : cmd.sectionId,
                    p_place_type : cmd.placeType,
                    p_user_type  : userType.id,
                    p_adv_user   : advancedUser,
            ].each { k, v ->
                registerParameter(k, v.class, ParameterMode.IN).bindValue(v)
            }
            outputs
        }
        ((ResultSetOutput) outputs.current).resultList.collect { item ->
            [id: item[0], name: item[1], seat: item[2], booking: item[3]]
        }
    }
}
