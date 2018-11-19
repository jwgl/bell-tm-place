package cn.edu.bnuz.bell.place

class BookingFormCommand {
    Long id
    String departmentId
    Long bookingTypeId
    String reason
    Integer numberOfUsers

    List<FormItem> addedItems

    List<Integer> removedItems

    class FormItem {
        Long id
        String placeId
        Integer startWeek
        Integer endWeek
        Integer oddEven
        Integer dayOfWeek
        Integer sectionId
    }
}
