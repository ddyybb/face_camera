//
//  FDCamTexture.swift
//  Runner
//
//  Created by Dmitriy Baranov on 01/03/2019.
//  Copyright © 2019 The Chromium Authors. All rights reserved.
//

import Foundation
import AVFoundation
import libkern
import Accelerate
import FirebaseMLVision

class FDCamTexture:
    NSObject,
    AVCaptureVideoDataOutputSampleBufferDelegate,
    FlutterTexture
{
    private var lastFrame: CMSampleBuffer?
    var onFrameAvailable: (() -> Void)?
    var faces: [VisionFace] = []
    
    private lazy var faceDetector: VisionFaceDetector = {
        let options = VisionFaceDetectorOptions()
        
        options.landmarkMode = .none
        options.contourMode = .all
        options.classificationMode = .none
        options.performanceMode = .fast
        
        let vision = Vision.vision()
        let faceDetector = vision.faceDetector(options: options)
        
        return faceDetector
    }()
    
    func captureOutput(
        _ output: AVCaptureOutput,
        didOutput buffer: CMSampleBuffer,
        from connection: AVCaptureConnection
        ) {
        var sampleBuffer: CMSampleBuffer!
        
        CMSampleBufferCreateCopy(kCFAllocatorDefault, buffer, &sampleBuffer)
        
        if sampleBuffer == nil {
            print("Failed to copy sample buffer.")
            return
        }
        
        guard let imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else {
            print("Failed to get image buffer from sample buffer.")
            return
        }
        
        lastFrame = sampleBuffer
        
        onFrameAvailable?()
        
        let visionImage = VisionImage(buffer: sampleBuffer)
        let imageWidth = CGFloat(CVPixelBufferGetWidth(imageBuffer))
        let imageHeight = CGFloat(CVPixelBufferGetHeight(imageBuffer))
        
        detectFacesOnDevice(
            in: visionImage,
            width: imageWidth,
            height: imageHeight
        )
    }
    
    func copyPixelBuffer() -> Unmanaged<CVPixelBuffer>? {
        guard
            let sampleBuffer = lastFrame,
            let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer)
            else {
                return nil
        }
        
        let paintedBuffer = draw(in: pixelBuffer)
        
        return Unmanaged.passRetained(paintedBuffer)
    }
    
    private func drawLips(
        context: CGContext,
        face: VisionFace,
        transform: CGAffineTransform
        ) {
        guard
            let top = face.contour(ofType: .upperLipBottom)?.points,
            let bottom = face.contour(ofType: .lowerLipTop)?.points
            else {
                return
        }
        
        (top + bottom).forEach { (point) in
            let rect = CGRect(
                origin: CGPoint(
                    x: point.x.intValue,
                    y: point.y.intValue
                ),
                size: CGSize(
                    width: 5,
                    height: 5
                )
                ).applying(transform)
            
            context.fillEllipse(in: rect)
        }
    }
    
    private func draw(in pixelBuffer: CVPixelBuffer) -> CVPixelBuffer {
        let imageWidth = CVPixelBufferGetWidth(pixelBuffer)
        let imageHeight = CVPixelBufferGetHeight(pixelBuffer)
        let transform = CGAffineTransform(
            translationX: 0,
            y: CGFloat(imageHeight)
            ).scaledBy(
                x: 1.0,
                y: -1.0
        )
        
        CVPixelBufferLockBaseAddress( pixelBuffer, .readOnly)
        let pixelData = CVPixelBufferGetBaseAddress( pixelBuffer)
        
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        
        if let context = CGContext(
            data: pixelData,
            width: imageWidth,
            height: imageHeight,
            bitsPerComponent: 8,
            bytesPerRow: 4 * imageWidth,
            space: colorSpace,
            bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue
            ) {
            UIGraphicsPushContext(context)
            context.saveGState()
            
            if let face = faces.first {
                drawLips(context: context, face: face, transform: transform)
            }
            
            UIGraphicsPopContext()
        }
        
        CVPixelBufferUnlockBaseAddress( pixelBuffer, .readOnly);
        
        return pixelBuffer
    }
    
    private func detectFacesOnDevice(
        in image: VisionImage,
        width: CGFloat,
        height: CGFloat
        ) {
        var detectedFaces: [VisionFace]? = nil
        print(width)
        print(height)
        
        do {
            detectedFaces = try faceDetector.results(in: image)
        } catch let error {
            print("Failed to detect faces with error: \(error.localizedDescription).")
        }
        guard let faces = detectedFaces, !faces.isEmpty else {
            print("On-Device face detector returned no results.")
            return
        }
        
        self.faces = faces
    }
}
