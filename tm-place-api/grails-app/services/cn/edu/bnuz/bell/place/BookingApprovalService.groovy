package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.NotFoundException
import cn.edu.bnuz.bell.organization.Teacher
import cn.edu.bnuz.bell.security.User
import cn.edu.bnuz.bell.workflow.Activities
import cn.edu.bnuz.bell.workflow.ListCommand
import cn.edu.bnuz.bell.workflow.ListType
import cn.edu.bnuz.bell.workflow.State
import cn.edu.bnuz.bell.workflow.WorkflowActivity
import cn.edu.bnuz.bell.workflow.WorkflowInstance
import cn.edu.bnuz.bell.workflow.Workitem
import cn.edu.bnuz.bell.workflow.commands.AcceptCommand
import cn.edu.bnuz.bell.workflow.commands.RejectCommand
import cn.edu.bnuz.bell.workflow.commands.RevokeCommand
import grails.gorm.transactions.Transactional
import org.springframework.orm.hibernate5.HibernateJdbcException

@Transactional
class BookingApprovalService extends BookingCheckService {
    private getCounts(String userId, ListType type, String query) {
        def counts = [
                (ListType.TODO): BookingForm.countByStatus(State.CHECKED),
                (ListType.DONE): BookingForm.countByApprover(Teacher.load(userId)),
                (ListType.TOBE): BookingForm.countByStatus(State.SUBMITTED),
        ]

        if (query) {
            switch (type) {
                case ListType.TODO:
                    counts.(ListType.TODO) = dataAccessService.getLong '''
select count(*)
from BookingForm form
join form.user user
where form.status = :status
and user.name like :query
''', [status: State.CHECKED, query: query]
                    break
                case ListType.DONE:
                    counts.(ListType.DONE) = dataAccessService.getLong '''
select count(*)
from BookingForm form
join form.user user
where form.approver.id = :userId
and user.name like :query
''', [userId: userId, query: query]
                    break
                case ListType.TOBE:
                    counts.(ListType.TOBE) = dataAccessService.getLong '''
select count(*)
from BookingForm form
join form.user user
where form.status = :status
and user.name like :query
''', [status: State.SUBMITTED, query: query]
                    break
            }
        }

        return counts
    }

    def list(String userId, ListCommand cmd) {
         switch (cmd.type) {
            case ListType.TODO:
                return findTodoList(userId, cmd)
            case ListType.DONE:
                return findDoneList(userId, cmd)
            case ListType.TOBE:
                return findTobeList(userId, cmd)
            default:
                throw new BadRequestException()
        }
    }

    def findTodoList(String userId, ListCommand cmd) {
        def forms = BookingForm.executeQuery '''
select new map(
  form.id as id,
  dept.name as department,
  type.name as type,
  user.id as userId,
  user.name as userName,
  form.reason as reason,
  form.dateChecked as date,
  form.status as status
)
from BookingForm form
join form.user user
join form.type type
join form.department dept
where form.status = :status
and user.name like :query
order by form.dateChecked
''', [status: State.CHECKED, query: cmd.query ?: '%'],
     [offset: cmd.offset, max: cmd.max]

        return [forms: forms, counts: getCounts(userId, cmd.type, cmd.query)]
    }

    def findDoneList(String userId, ListCommand cmd) {
        def forms = BookingForm.executeQuery '''
select new map(
  form.id as id,
  dept.name as department,
  type.name as type,
  user.id as userId,
  user.name as userName,
  form.reason as reason,
  form.dateApproved as date,
  form.status as status,
  form.report.id as reportId
)
from BookingForm form
join form.user user
join form.type type
join form.department dept
where form.approver.id = :userId
and user.name like :query
order by form.dateApproved desc
''', [userId: userId, query: cmd.query ?: '%'],
     [offset: cmd.offset, max: cmd.max]

        return [forms: forms, counts: getCounts(userId, cmd.type, cmd.query)]
    }

    def findTobeList(String userId, ListCommand cmd) {
        def forms = BookingForm.executeQuery '''
select new map(
  form.id as id,
  dept.name as department,
  type.name as type,
  user.id as userId,
  user.name as userName,
  form.reason as reason,
  form.dateSubmitted as date,
  form.status as status
)
from BookingForm form
join form.user user
join form.type type
join form.department dept
where form.status = :status
and user.name like :query
order by form.dateSubmitted desc
''', [status: State.SUBMITTED, query: cmd.query ?: '%'],
     [offset: cmd.offset, max: cmd.max]

        return [forms: forms, counts: getCounts(userId, cmd.type, cmd.query)]
    }

    def getFormForApproval(String userId, Long id, ListType type, String query) {
        def form = bookingFormService.getFormInfo(id)

        if (!form) {
            throw new NotFoundException()
        }

        def activity = Activities.APPROVE
        def workitem = Workitem.findByInstanceAndActivityAndToAndDateProcessedIsNull(
                WorkflowInstance.load(form.workflowInstanceId),
                WorkflowActivity.load("${BookingForm.WORKFLOW_ID}.${activity}"),
                User.load(userId),
        )
        domainStateMachineHandler.checkReviewer(id, userId, activity)

        form.extraInfo = getUserExtraInfo(form)
        return [
                form: form,
                counts: getCounts(userId, type, query),
                workitemId: workitem ? workitem.id : null,
                prevId: getPrevApprovalId(userId, id, type, query),
                nextId: getNextApprovalId(userId, id, type, query),
        ]
    }

    def getFormForApproval(String userId, Long id, ListType type, UUID workitemId) {
        def form = bookingFormService.getFormInfo(id)

        if (!form) {
            throw new NotFoundException()
        }

        def activity = Activities.APPROVE
        domainStateMachineHandler.checkReviewer(id, userId, activity)

        form.extraInfo = getUserExtraInfo(form)
        return [
                form: form,
                counts: getCounts(userId, type, null),
                workitemId: workitemId,
                prevId: getPrevApprovalId(userId, id, type, null),
                nextId: getNextApprovalId(userId, id, type, null),
        ]
    }

    private Long getPrevApprovalId(String userId, Long id, ListType type, String query) {
        switch (type) {
            case ListType.TODO:
                return dataAccessService.getLong('''
select form.id
from BookingForm form
join form.user user
where form.status = :status
and user.name like :query
and form.dateChecked < (select dateChecked from BookingForm where id = :id)
order by form.dateChecked desc
''', [id: id, status: State.CHECKED, query: query ?: '%'])
            case ListType.DONE:
                return dataAccessService.getLong('''
select form.id
from BookingForm form
join form.user user
where form.approver.id = :userId
and user.name like :query
and form.dateApproved > (select dateApproved from BookingForm where id = :id)
order by form.dateApproved asc
''', [id: id, userId: userId, query: query ?: '%'])
            case ListType.TOBE:
                return dataAccessService.getLong('''
select form.id
from BookingForm form
join form.user user
where form.status = :status
and user.name like :query
and form.dateSubmitted > (select dateSubmitted from BookingForm where id = :id)
order by form.dateSubmitted asc
''', [id: id, status: State.SUBMITTED, query: query ?: '%'])
        }
    }

    private Long getNextApprovalId(String userId, Long id, ListType type, String query) {
        switch (type) {
            case ListType.TODO:
                return dataAccessService.getLong('''
select form.id
from BookingForm form
join form.user user
where form.status = :status
and user.name like :query
and form.dateChecked > (select dateChecked from BookingForm where id = :id)
order by form.dateChecked asc
''', [id: id, status: State.CHECKED, query: query ?: '%'])
            case ListType.DONE:
                return dataAccessService.getLong('''
select form.id
from BookingForm form
join form.user user
where form.approver.id = :userId
and user.name like :query
and form.dateApproved < (select dateApproved from BookingForm where id = :id)
order by form.dateApproved desc
''', [id: id, userId: userId, query: query ?: '%'])
            case ListType.TOBE:
                return dataAccessService.getLong('''
select form.id
from BookingForm form
join form.user user
where form.status = :status
and user.name like :query
and form.dateSubmitted < (select dateSubmitted from BookingForm where id = :id)
order by form.dateSubmitted desc
''', [id: id, status: State.SUBMITTED, query: query ?: '%'])
        }
    }

    void accept(String userId, AcceptCommand cmd, UUID workitemId) {
        BookingForm form = BookingForm.get(cmd.id)

        if (!checkOccupation(form)) {
            return
        }

        domainStateMachineHandler.accept(form, userId, Activities.APPROVE, cmd.comment, workitemId)
        form.approver = Teacher.load(userId)
        form.dateApproved = new Date()
        form.save(flush: true)

        try {
            insertSyncForm(form.id)
        } catch(HibernateJdbcException e) {
            println e.rootCause.message
            throw e
        }
    }

    def insertSyncForm(Long id) {
        def results = BookingForm.executeQuery '''
select new map(
  form.id as formId,
  form.term.id / 10 || '-' || form.term.id / 10 + 1 as schoolYear,
  cast(form.term.id % 10 as string) as term,
  place.id as placeId,
  place.name as placeName,
  item.startWeek as startWeek,
  item.endWeek as endWeek,
  case item.oddEven when 0 then null when 1 then '单' when 2 then '双' end as oddEven,
  item.dayOfWeek as dayOfWeek,
  section.start as startSection,
  section.total as totalSection,
  section.name as sectionName,
  department.name as departmentName,
  user.id as userId,
  user.name as userName,
  user.longPhone as userPhone,
  form.reason as reason,
  to_char(current_timestamp, 'yyyy-mm-dd HH24:MI:SS') as bookingDate,
  'Tm' as source,
  checker.officePhone as checkerPhone,
  form.approver.id as approverId,
  '4' as status
)
from BookingForm form
join form.items item
join item.place place
join item.section itemSection
join form.department department
join form.user user
join form.checker,
     BookingSection section,
     User checker
where checker.id = form.checker.id
  and section.id = any_element(itemSection.includes)
  and form.id = :id
''', [id: id]

        results.each {Map item ->
            new SyncBookingForm(item).save()
        }
    }

    void reject(String userId, RejectCommand cmd, UUID workitemId) {
        BookingForm form = BookingForm.get(cmd.id)
        domainStateMachineHandler.reject(form, userId, Activities.APPROVE, cmd.comment, workitemId)
        form.approver = Teacher.load(userId)
        form.dateApproved = new Date()
        form.save()
    }

    void revoke(String userId, RevokeCommand cmd) {
        BookingForm form = BookingForm.get(cmd.id)

        if (!form) {
            throw new NotFoundException()
        }

        if (!domainStateMachineHandler.canRevoke(form)) {
            throw new BadRequestException()
        }

        domainStateMachineHandler.checkReviewer(cmd.id, userId, Activities.APPROVE)

        domainStateMachineHandler.revoke(form, userId, cmd.comment)

        form.save()
        deleteSyncForm(form.id)
    }

    def deleteSyncForm(Long formId) {
        def forms = SyncBookingForm.findAllByFormId(formId)
        forms.each {form ->
            form.delete()
        }
    }
}

