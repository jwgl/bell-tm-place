package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.ForbiddenException
import cn.edu.bnuz.bell.http.NotFoundException
import cn.edu.bnuz.bell.master.Term
import cn.edu.bnuz.bell.organization.Department
import cn.edu.bnuz.bell.security.SecurityService
import cn.edu.bnuz.bell.security.User
import cn.edu.bnuz.bell.security.UserType
import cn.edu.bnuz.bell.tm.common.master.TermService
import cn.edu.bnuz.bell.workflow.DomainStateMachineHandler
import cn.edu.bnuz.bell.workflow.commands.SubmitCommand
import grails.transaction.Transactional
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
  ck.name as checker,
  bf.dateChecked as dateChecked,
  ap.name as approver,
  bf.dateApproved as dateApproved,
  bf.status as status
)
from BookingForm bf
join bf.department bd
join bf.type bt
left join bf.checker ck
left join bf.approver ap
where bf.user.id = :userId
order by bf.dateModified desc
''', [userId: userId], [offset: offset, max: max]
    }

    /**
     * 获取用户申请单数量
     * @param userId 用户ID
     * @return 数量
     */
    def formCount(String userId) {
        BookingForm.countByUser(User.load(userId))
    }

    def getFormInfo(Long id) {
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
  section.name as sectionName
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
                            id  : it.sectionId,
                            name: it.sectionName,
                    ]
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

        form.editable = domainStateMachineHandler.canUpdate(form)

        return form
    }

    def getFormForCreate(String userId) {
        def user = User.get(userId)
        def term = termService.activeTerm
        def departments = getBookingDepartments(user.departmentId)
        def department = departments.find { it.departmentId == user.departmentId }
        def departmentId = department ? department.id : departments[0].id
        def bookingTypes = getBookingTypes(departmentId)

        return [
                term        : [
                        startWeek  : term.startWeek,
                        maxWeek    : term.maxWeek,
                        currentWeek: term.currentWorkWeek,
                        startDate  : term.startDate,
                        swapDates  : term.swapDates
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
                        swapDates  : term.swapDates
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
where dept.id = :departmentId or dept.isTeaching = false
order by dept.id
''', [departmentId: departmentId]
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

    private getBookingDays(UserType userType) {
        if (userType == UserType.TEACHER) {
            if (securityService.hasRole('ROLE_BOOKING_ADV_USER')) {
                return -1
            } else {
                0
            }
        } else {
            return 14
        }
    }

    def getPlaceTypes(UserType userType) {
        // TODO: 考虑允许学期和部门
        Place.executeQuery '''
select distinct place.type
from PlaceUserType put
join put.place place
where put.userType = :userType
order by place.type
''', [userType: userType]
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
                    p_user_type  : userType.id
            ].each { k, v ->
                registerParameter(k, v.class, ParameterMode.IN).bindValue(v)
            }
            outputs
        }
        ((ResultSetOutput) outputs.current).resultList.collect { item ->
            [id: item[0], name: item[1], seat: item[2], booking: item[3]]
        }
    }

    def getCheckers(Long id) {
        BookingAuth.executeQuery '''
select new map(
  checker.id as id,
  checker.name as name
)
from BookingAuth bc
join bc.checker checker
where (bc.type, bc.department) in (
  select form.type, form.department
  from BookingForm form
  where form.id = :id
)''', [id: id]
    }

    def export(String userId, Date start, Date end) {
        BookingForm.executeQuery '''
select new map(
  form.id as id,
  form.term.id as term,
  item.startWeek as startWeek,
  item.endWeek as endWeek,
  item.oddEven as oddEven,
  item.dayOfWeek as dayOfWeek,
  section.name as section,
  place.name as place,
  form.status as status,
  type.name as type,
  dept.name as department,
  form.userName as userName,
  form.dateCreated as dateCreated,
  form.reason as reason
)
from BookingForm form
join form.department dept
join form.type type
join form.items item
join form.section section
join item.place place
where cast(form.dateCreated as date) between :start and :end
and form.userId = :userId
and form.status != 0
order by form.id, item.startWeek, item.dayOfWeek, item.oddEven, section.id, room.name
''', [userId: userId, start: start, end: end]
    }
}
