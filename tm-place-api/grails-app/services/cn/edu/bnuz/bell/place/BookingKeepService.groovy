package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.master.Term
import cn.edu.bnuz.bell.place.dto.BuildingBooking
import grails.gorm.transactions.Transactional

@Transactional
class BookingKeepService {
    def findBookings(Term term, String building, String placeId, Integer week, Integer dayOfWeek, Integer section) {
        BuildingBooking.findBuildingBookings(term, building, placeId, week, dayOfWeek, section)
    }

    def findBuildings(Term term) {
        Place.executeQuery '''
select distinct place.building
from PlaceUserType placeUserType
join placeUserType.place place
left join place.allowBookingTerms placeBookingTerm
where (place.enabled = true or placeBookingTerm.term = :term)
order by place.building
''', [term: term]
    }

    def findPlaces(Term term, String building) {
        Place.executeQuery '''
select distinct new map(
    place.id as id,
    place.name as name,
    place.seat as seat,
    place.type as type
)
from PlaceUserType placeUserType
join placeUserType.place place
left join place.allowBookingTerms placeBookingTerm with placeBookingTerm.term = :term
where (place.enabled = true or placeBookingTerm.term is not null)
and building = :building
order by place.name
''', [term: term, building: building]
    }
}
