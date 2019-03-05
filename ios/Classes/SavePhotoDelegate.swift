//
//  SavePhotoDelegate.swift
//  face_camera
//
//  Created by Dmitriy Baranov on 05/03/2019.
//

import AVFoundation

class SavePhotoDelegate: NSObject, AVCapturePhotoCaptureDelegate {
    let rect: CGRect
    var selfReference: SavePhotoDelegate?
    
    init(face: CGRect) {
        rect = face
        super.init()
        selfReference = self
    }
    
    func photoOutput(
        _ output: AVCapturePhotoOutput,
        didFinishProcessingPhoto photoSampleBuffer: CMSampleBuffer?,
        previewPhoto previewPhotoSampleBuffer: CMSampleBuffer?,
        resolvedSettings: AVCaptureResolvedPhotoSettings,
        bracketSettings: AVCaptureBracketedStillImageSettings?,
        error: Error?
    ) {
        selfReference = nil
        
        if error != nil {
            print(error?.localizedDescription)
            return
        }
        
        guard let photoSampleBuffer = photoSampleBuffer else {
            print("photo sample buffer is empty")
            return
        }
        
        guard let data = AVCapturePhotoOutput.jpegPhotoDataRepresentation(
            forJPEGSampleBuffer: photoSampleBuffer,
            previewPhotoSampleBuffer: previewPhotoSampleBuffer
            ) else {
                print("cannot get jpegPhotoDataRepresentation from sample buffer")
                return
        }
        
        guard let image = UIImage(data: data) else {
            print("cannot get uiimage from jpegPhotoDataRepresentation")
            return
        }
        
        guard let rotated = rotate(image: image, byDegrees: 90) else {
            print("cannot rotate the image")
            return
        }
        
        if let cropped = rotated.cgImage?.cropping(to: rect) {
            UIImageWriteToSavedPhotosAlbum(UIImage(cgImage: cropped), nil, nil, nil)
        }
    }
    
    private func rotate(image: UIImage, byDegrees: Double) -> UIImage? {
        UIGraphicsBeginImageContext(image.size)
        let bitmap = UIGraphicsGetCurrentContext()
        
        bitmap?.translateBy(
            x: image.size.width / 2,
            y: image.size.height / 2
        )
        
        bitmap?.rotate(by: CGFloat(byDegrees * .pi / 180))
        
        let rect = CGRect(
            x: -image.size.height / 2,
            y: -image.size.width / 2,
            width: image.size.height,
            height: image.size.width
        )
        
        bitmap?.draw(image.cgImage!, in: rect)
        
        let newImage = UIGraphicsGetImageFromCurrentImageContext()
        
        UIGraphicsEndImageContext();
        
        return newImage
    }
}

