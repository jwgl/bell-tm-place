package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.workflow.State
import grails.transaction.Transactional

@Transactional
class BookingService {
    def findAllByStatus(State status) {
        BookingForm.executeQuery '''
select new map(
  form.id as id,
  userId.id as userId,
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
left join form.checker checker
left join form.approver approver
where form.status = :status
order by form.dateModified desc
''', [status: status]
    }
}
