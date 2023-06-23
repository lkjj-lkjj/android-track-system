package com.example.xiao.test1.utils;

import com.amap.api.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class PathData {
    public LatLng latLng;
    public int turn = 0;
    /*
    非楼梯：0
    上楼梯：1 red
    下楼梯：2 yellow

     */
    public int stair = 0;
    public int steps = 0;
    public int stage = 0;
}
