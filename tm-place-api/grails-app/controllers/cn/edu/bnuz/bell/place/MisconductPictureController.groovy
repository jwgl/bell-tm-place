package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.BadRequestException
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.util.exif.ExifUtils
import net.coobird.thumbnailator.util.exif.Orientation
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.multipart.MultipartFile

import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.stream.ImageInputStream
import java.nio.file.Files

class MisconductPictureController {
    @Value('${bell.booking.misconduct.picturePath}')
    String picturePath

    def index() {}

    def show(String id) {
        File file = new File(picturePath, "${id}.${params.format}")
        if (file.exists()) {
            render file: file.newInputStream(), contentType: Files.probeContentType(file.toPath())
        } else {
            render status: HttpStatus.NOT_FOUND
        }
    }

    def save() {
        MultipartFile uploadFile = request.getFile('file')
        if (!uploadFile.empty) {
            def ext = uploadFile.originalFilename.substring(uploadFile.originalFilename.lastIndexOf('.') + 1).toLowerCase()
            def localFile = new File(picturePath, "${UUID.randomUUID()}.${ext}")
            if (shouldRotate(uploadFile.inputStream)) {
                Thumbnails.of(uploadFile.inputStream)
                        .height(1110)
                        .keepAspectRatio(true)
                        .toFile(localFile)
            } else {
                Thumbnails.of(uploadFile.inputStream)
                        .width(1110)
                        .keepAspectRatio(true)
                        .toFile(localFile)
            }
            renderJson([file: localFile.name])
        } else {
            throw new BadRequestException('Empty file.')
        }
    }

    private def shouldRotate(InputStream is) {
        ImageInputStream iis = ImageIO.createImageInputStream(is)
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis)
        ImageReader reader = readers.next()
        reader.setInput(iis)
        try {
            Orientation orientation = ExifUtils.getExifOrientation(reader, 0);
            if (orientation != null && orientation != Orientation.TOP_LEFT) {
                return true
            }
            return false
        } catch (e) {
            return false
        }
    }
}
