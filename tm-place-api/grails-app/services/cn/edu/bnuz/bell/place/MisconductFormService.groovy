package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.ForbiddenException
import cn.edu.bnuz.bell.http.NotFoundException
import cn.edu.bnuz.bell.organization.Teacher
import grails.gorm.transactions.Transactional

@Transactional
class MisconductFormService {

    def getForms(String keeperId, Long bookingItemId) {
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
''', [bookingItemId: bookingItemId]

        if (!bookings) {
            throw new NotFoundException()
        }

        def forms = BookingMisconduct.executeQuery '''
select new map(
  bm.id as id,
  bm.type as type,
  bm.content as content,
  bm.pictures as pictures,
  bm.week as week,
  bm.userCount as userCount,
  bm.status as status,
  reporter.id as reporterId,
  reporter.name as reporterName,
  bm.dateModified as dateModified,
  case when reporter.id = :keeperId and bm.status = 0 then true else false end as editable
)
from BookingMisconduct bm
join bm.bookingItem bi
join bm.reporter reporter
where bi.id = :bookingItemId
''', [bookingItemId: bookingItemId, keeperId: keeperId]
        return [
                booking: bookings[0],
                forms  : forms,
        ]
    }

    def getForm(Long id) {
        def misconducts = BookingMisconduct.executeQuery '''
select new map(
  bm.id as id,
  bm.type as type,
  bm.content as content,
  bm.pictures as pictures,
  bm.week as week,
  bm.userCount as userCount,
  bm.status as status,
  reporter.id as reporterId,
  reporter.name as reporterName,
  bm.dateModified as dateModified,
  case when bm.status = 0 then true else false end as editable
)
from BookingMisconduct bm
join bm.reporter reporter
where bm.id = :id
''', [id: id]
        return misconducts[0]
    }

    def create(String keeperId, Long bookingItemId, MisconductCommand cmd) {
        def now = new Date()
        BookingMisconduct form = new BookingMisconduct([
                bookingItem : BookingItem.load(bookingItemId),
                type        : cmd.type,
                content     : cmd.content,
                week        : cmd.week,
                userCount   : cmd.userCount,
                pictures    : cmd.pictures.toArray(),
                reporter    : Teacher.load(keeperId),
                dateCreated : now,
                dateModified: now,
                status      : 0,
        ])
        form.save()

        return getForm(form.id)
    }

    def update(String keeperId, Long bookingItemId, MisconductCommand cmd) {
        BookingMisconduct form = BookingMisconduct.get(cmd.id)
        if (form.reporterId != keeperId) {
            throw new ForbiddenException()
        }

        if (form.bookingItemId != bookingItemId || form.status != 0) {
            throw new BadRequestException()
        }

        form.type = cmd.type
        form.content = cmd.content
        form.pictures = cmd.pictures
        form.week = cmd.week
        form.userCount = cmd.userCount
        form.dateModified = new Date()

        form.save(flush: true)

        return getForm(form.id)
    }

    def delete(String keeperId, Long bookingItemId, Long id) {
        BookingMisconduct form = BookingMisconduct.get(id)

        if (form.reporterId != keeperId) {
            throw new ForbiddenException()
        }

        if (form.bookingItemId != bookingItemId || form.status != 0) {
            throw new BadRequestException()
        }

        form.delete()
    }
}
