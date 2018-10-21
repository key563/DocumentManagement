"""
均值滤波
"""
import numpy as np
import cv2


# 定义函数，生成椒盐噪声图像
def salt_pepperNoise(src):
    dst = src.copy()
    num = 1000  # 1000个噪声点
    ndim = np.ndim(src)
    row, col = np.shape(src)[0:2]
    for i in range(num):
        x = np.random.randint(0, row)  # 随机生成噪声点位置
        y = np.random.randint(0, col)
        indicator = np.random.randint(0, 2)  # 生成随机数0和1，决定是椒噪声还是盐噪声
        # 灰度图像
        if ndim == 2:
            if indicator == 0:
                dst[x, y] = 0
            else:
                dst[x, y] = 255
        # 彩色图像
        elif ndim == 3:
            if indicator == 0:
                dst[x, y, :] = 0
            else:
                dst[x, y, :] = 255
    return dst


# 定义函数，实现均值滤波
def meanFilter(src, wsize):  # src为输入图像，wsize为窗口大小
    border = np.uint8(wsize/2.0)  # 计算扩充边缘
    addBorder = cv2.copyMakeBorder(src, border, border, border, border, cv2.BORDER_REFLECT_101)  # 扩充后
    dst = src.copy()
    filterWin = 1.0/(wsize**2) * np.ones((wsize, wsize), dtype=np.float32)  # 定义窗口
    row, col = np.shape(addBorder)
    # 滑动，开始滤波
    for i in range(border, row-border):
        for j in range(border, col-border):
            temp = addBorder[i-border:i+border+1, j-border:j+border+1]
            newValue = np.sum(temp * filterWin)  # 均值滤波
            dst[i-border, j-border] = newValue
    dst = np.uint8(dst + 0.5)
    return dst


img = cv2.imread('d:/temp/lan.jpeg', cv2.IMREAD_GRAYSCALE)
# 生成椒盐图
saltPimg = salt_pepperNoise(img)
cv2.imshow('saltPepper', saltPimg)
# 均值滤波
MeanFimg = meanFilter(saltPimg, 3)
cv2.imshow('MeanFilter', MeanFimg)
cv2.waitKey(0)
cv2.destroyAllWindows()
