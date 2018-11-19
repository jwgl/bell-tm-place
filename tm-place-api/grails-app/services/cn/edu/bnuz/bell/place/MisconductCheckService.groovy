package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.NotFoundException
import cn.edu.bnuz.bell.organization.Teacher
import cn.edu.bnuz.bell.service.DataAccessService
import grails.gorm.transactions.Transactional

@Transactional
class MisconductCheckService {
    DataAccessService dataAccessService

    def list(String userId, String status, Integer offset, Integer max) {
        switch (status) {
            case 'todo':
                return todoList(userId, offset, max)
            case 'done':
                return doneList(userId, offset, max)

        }
    }

    def countByStatus(String userId, String status) {
        switch (status) {
            case 'todo':
                return todoCount(userId)
            case 'done':
                return doneCount(userId)

        }
    }

    def counts(String userId) {
        [
                todo: todoCount(userId),
                done: doneCount(userId),
        ]
    }

    def todoCount(String userId) {
        dataAccessService.getLong '''
select count(bm.id)
from BookingMisconduct bm
join bm.bookingItem bi
join bi.form bf
join bf.department department
join bf.type bt
join bf.department dept
join bt.auths ba
where ba.department = dept
and ba.checker.id = :userId
and bm.status = :status
''', [userId: userId, status: 1]
    }

    def doneCount(String userId) {
        BookingMisconduct.countByChecker(Teacher.load(userId))
    }

    def todoList(String userId, Integer offset, Integer max) {
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
    checker.name as checker
)
from BookingMisconduct bm
join bm.bookingItem bi
join bi.form bf
join bi.section bs
join bi.place place
join bf.department department
join bf.user user
join bf.checker checker
join bf.type bt
join bf.department dept
join bt.auths ba
where ba.department = dept
and ba.checker.id = :userId
and bm.status = :status
order by bm.dateHandled
''', [userId: userId, status: 1], [offset: offset, max: max]
    }

    def doneList(String userId, Integer offset, Integer max) {
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
    checker.name as checker
)
from BookingMisconduct bm
join bm.bookingItem bi
join bi.form bf
join bi.section bs
join bi.place place
join bf.department department
join bf.user user
join bf.checker checker
where bm.checker.id = :userId
order by coalesce(bm.dateApproved, bm.dateChecked) desc
''', [userId: userId], [offset: offset, max: max]
    }

    def getForm(String userId, Long id) {
        def misconducts = BookingMisconduct.executeQuery '''
select new map(
  bm.id as id,
  bi.id as bookingItemId,
  bm.type as type,
  bm.week as week,
  bm.userCount as userCount,
  bm.content as content,
  bm.pictures as pictures,
  bm.handleOutcome as handleOutcome,
  bm.checkOutcome as checkOutcome,
  bm.approvalOutcome as approvalOutcome,
  bm.status as status
)
from BookingMisconduct bm
join bm.bookingItem bi
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
join bf.department dept
join bt.auths ba
where ba.department = dept
and ba.checker.id = :userId
and bi.id = :bookingItemId
''', [userId: userId, bookingItemId: misconducts[0].bookingItemId]

        if (!bookings) {
            throw new NotFoundException()
        }

        return [
                booking   : bookings[0],
                misconduct: misconducts[0],
        ]
    }

    def updateStatus(String userId, Long id, String outcome) {
        BookingMisconduct form = BookingMisconduct.get(id)

        if (!form) {
            throw new NotFoundException()
        }

        form.status = 2
        form.checkOutcome = outcome
        form.checker = Teacher.load(userId)
        form.dateChecked = new Date()

        form.save(flush: true)

        return [count: doneCount(userId)]
    }
}
