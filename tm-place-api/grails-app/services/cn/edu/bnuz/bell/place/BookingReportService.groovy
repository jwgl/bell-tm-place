package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.NotFoundException
import cn.edu.bnuz.bell.organization.Teacher
import cn.edu.bnuz.bell.workflow.State
import grails.transaction.Transactional

@Transactional
class BookingReportService {

    def getAll() {
        BookingReport.executeQuery '''
select new map(
  report.id as id,
  creator.name as creator,
  report.dateCreated as dateCreated,
  modifier.name as modifier,
  report.dateModified as dateModified
)
from BookingReport report
join report.creator creator
left join report.modifier modifier
order by report.id desc
'''
    }

    def getInfo(Long id) {
        def results = BookingReport.executeQuery '''
select new map(
  report.id as id,
  creator.name as creator,
  report.dateCreated as dateCreated,
  modifier.name as modifier,
  report.dateModified as dateModified
)
from BookingReport report
join report.creator creator
left join report.modifier modifier
where report.id = :id
''', [id: id]

        if (!results) {
            throw new NotFoundException()
        }

        def report = results[0]
        report.items = BookingForm.executeQuery '''
select new map(
  form.id as formId,
  user.name as userName,
  form.reason as reason,
  department.name as department,
  checker.name as checker,
  form.dateChecked as dateChecked,
  approver.name as approver,
  form.dateApproved as dateApproved,
  form.status as status
)
from BookingForm form
join form.user user
join form.department department
join form.checker checker
join form.approver approver
where form.report.id = :id
order by form.dateApproved desc
''', [id: id]

        return report
    }

    BookingReport create(String userId, BookingReportCommand cmd) {
        BookingReport report = new BookingReport(
                creator: Teacher.load(userId)
        )

        cmd.addedItems.each {
            report.addToForms(BookingForm.get(it))
        }

        report.save()
    }

    void update(String userId, BookingReportCommand cmd) {
        BookingReport report = BookingReport.get(cmd.id)
        report.modifier = Teacher.load(userId)
        report.dateModified = new Date()

        cmd.addedItems.each {
            report.addToForms(BookingForm.get(it))
        }

        cmd.removedItems.each {
            report.removeFromForms(BookingForm.get(it))
        }

        report.save()
    }

    void delete(Long id) {
        def report = BookingReport.get(id)
        report.delete()
    }

    def findUnreportedForms() {
        BookingForm.executeQuery '''
select new map(
  form.id as formId,
  user.id as userId,
  user.name as userName,
  form.reason as reason,
  department.name as department,
  checker.name as checker,
  form.dateChecked as dateChecked,
  approver.name as approver,
  form.dateApproved as dateApproved,
  form.status as status
)
from BookingForm form
join form.user user
join form.department department
join form.checker checker
join form.approver approver
where form.status = :status
and form.report is null
order by form.dateApproved desc
''', [status: State.APPROVED]
    }

}
