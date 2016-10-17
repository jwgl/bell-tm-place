package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.master.Term

class PlaceUsage {
    Term term
    Place place
    Integer startWeek
    Integer endWeek
    Integer oddEven
    Integer dayOfWeek
    Integer startSection
    Integer totalSection
    String type
    String department
    String description

    static mapping = {
        table 'dv_place_usage'
    }
}
