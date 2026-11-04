package com.gathin.tvhome;

/** Stable saved IDs; the names describe inspired variants, not OEM implementations. */
enum VolumeStyle {
    GLASS("glass",300,68), SONY("sony",300,64), LG("lg",122,166),
    PHILIPS("philips",340,74), SAMSUNG("samsung",80,254);
    static final String KEY="cec_volume_style";
    final String id;
    final int width,height;
    VolumeStyle(String id,int width,int height){this.id=id;this.width=width;this.height=height;}
    static VolumeStyle parse(String value){for(VolumeStyle s:values())if(s.id.equals(value))return s;return GLASS;}
    int label(){switch(this){case SONY:return R.string.volume_style_sony;case LG:return R.string.volume_style_lg;
        case PHILIPS:return R.string.volume_style_philips;case SAMSUNG:return R.string.volume_style_samsung;
        default:return R.string.volume_style_glass;}}
}
