package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.NotFoundException
import cn.edu.bnuz.bell.organization.Teacher
import grails.gorm.transactions.Transactional

@Transactional
class MisconductApprovalService {
    def list(Integer status, Integer offset, Integer max) {
        BookingMisconduct.executeQuery '''
select new map(
    bm.id as id,
    bm.type as type,
    bm.week as week,
    bf.numberOfUsers as numberOfUsers,
    bm.userCount as userCount,
    bi.dayOfWeek as dayOfWeek,
    bs.name as bookingSection,
    place.name as place,
    department.name as department,
    user.name as userName,
    checker.name as checker,
    reporter.name as reporter
)
from BookingMisconduct bm
join bm.bookingItem bi
join bi.form bf
join bi.section bs
join bi.place place
join bm.reporter reporter
join bf.department department
join bf.user user
join bf.checker checker
where bm.status = :status
order by coalesce(bm.dateApproved, bm.dateChecked, bm.dateHandled) desc, bm.dateModified
''', [status: status], [offset: offset, max: max]
    }

    def countByStatus(Integer status) {
        BookingMisconduct.countByStatus(status)
    }

    def counts() {
        BookingMisconduct.executeQuery('''
select new map(
  bm.status as status,
  count(bm.id) as count
)
from BookingMisconduct bm
group by bm.status
''').collectEntries { [it.status, it.count] }
    }

    def getForm(Long id) {
        def misconducts = BookingMisconduct.executeQuery '''
select new map(
  bm.id as id,
  bi.id as bookingItemId,
  bm.type as type,
  bm.week as week,
  bm.userCount as userCount,
  bm.content as content,
  bm.pictures as pictures,
  bm.dateModified as dateModified,
  bm.handleOutcome as handleOutcome,
  bm.checkOutcome as checkOutcome,
  bm.approvalOutcome as approvalOutcome,
  bm.status as status
)
from BookingMisconduct bm
join bm.bookingItem bi
join bm.reporter reporter
where bm.id = :id
''', [id: id]

        if (!misconducts) {
            throw new NotFoundException()
        }

        def bookings = BookingForm.executeQuery '''
select new map(
  bi.id as bookingItemId,
  bf.id as bookingFormId,
  bt.name as bookingType,
  place.name as place,
  bi.startWeek as startWeek,
  bi.endWeek as endWeek,
  bi.oddEven as oddEven,
  bi.dayOfWeek as dayOfWeek,
  bs.name as bookingSection,
  department.name as department,
  user.name as userName,
  checker.name as checkerName,
  user.longPhone as phoneNumber,
  bf.numberOfUsers as numberOfUsers,
  bf.reason as reason
)
from BookingForm bf
join bf.items bi
join bi.section bs
join bf.type bt
join bi.place place
join bf.department department
join bf.user user
join bf.checker checker
where bi.id = :bookingItemId
''', [bookingItemId: misconducts[0].bookingItemId]

        if (!bookings) {
            throw new NotFoundException()
        }

        return [
                booking   : bookings[0],
                misconduct: misconducts[0],
        ]
    }

    def updateStatus(String userId, Long id, Integer status, String outcome) {
        BookingMisconduct form = BookingMisconduct.get(id)

        if (!form) {
            throw new NotFoundException()
        }

        switch (form.status) {
            case 0:
                form.handleOutcome = outcome
                form.handler = Teacher.load(userId)
                form.dateHandled = new Date()
                break
            case 2:
                form.approvalOutcome = outcome
                form.approver = Teacher.load(userId)
                form.dateApproved = new Date()
                break
            default:
                throw new BadRequestException()
        }
        form.status = status
        form.save(flush: true)

        return [count: countByStatus(status)]
    }
}
