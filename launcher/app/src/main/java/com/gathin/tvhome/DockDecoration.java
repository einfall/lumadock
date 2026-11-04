package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/** Draws the tvOS style lighter band behind the first (dock) row and adds a gap below it. */
public class DockDecoration extends RecyclerView.ItemDecoration {

    private final int dockSize;
    private final float density;
    private final Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private DockStyle custom;
    private final Paint customFill=new Paint(3),customEdge=new Paint(3),glint=new Paint(3),innerEdge=new Paint(3);
    private final RectF inner=new RectF();
    private final RectF painted=new RectF();
    private boolean paintValid;
    private final QuickAccessDock quickAccess=new QuickAccessDock();
    DockDecoration(int size,float density,boolean translucent,boolean names,int opacity,boolean lightGlass,boolean evenPadding,boolean visible,boolean singleRow,boolean bandAll,DockStyle style){this(size,density,translucent,names,opacity,lightGlass,evenPadding,visible,singleRow,bandAll);setStyle(style);}
    void setStyle(DockStyle style){custom=style;paintValid=false;if(style==null||!"quickaccess".equals(style.mode))quickAccess.clear();}


    /** Extra band below the tiles for their names (always_show_names), in px. */
    private final float namesRoom;
    /** Light glass like the top bar's pills (config.xml dock_glass): a white tint that fades downwards. */
    private final boolean lightGlass;
    /** Padding all round the tiles equal to the gap between them (config.xml dock_even_padding). */
    private final boolean evenPadding;
    /** The band's top and bottom the glass gradient was made for; it is remade when they move. */
    private float shaderTop = Float.NaN, shaderBottom = Float.NaN;
    /** Draw the band and its edge (config.xml dock_band); false leaves the tiles floating on the wallpaper. */
    private final boolean bandVisible;
    /** single_row: the tiles after the dock share its row, so they take the same offsets to stay level with it. */
    private final boolean singleRow;
    /** dock_band_all: in a single row, the band runs behind every app instead of the dock apps only. */
    private final boolean bandAll;

    /**
     * @param translucent the cinematic shelf's band: the artwork and the glow show through it.
     * @param names       the dock tiles show their names, so the band reaches down over them.
     * @param opacity     the plain band's opacity in percent (dock_opacity), so a wallpaper can show through it.
     * @param lightGlass  a light frosted band like the pills, for a sky or wallpaper behind it.
     * @param evenPadding the band's padding is the gap between the tiles on all four sides (dock_even_padding).
     * @param bandVisible false skips the band and its edge, so the tiles float on the wallpaper (dock_band).
     * @param singleRow   every app sits in the dock's row (single_row); the band covers the dock apps only, unless bandAll.
     * @param bandAll     with singleRow, the band runs behind every app in the row (dock_band_all).
     */
    DockDecoration(int dockSize, float density, boolean translucent, boolean names, int opacity, boolean lightGlass,
                   boolean evenPadding, boolean bandVisible, boolean singleRow, boolean bandAll) {
        this.dockSize = dockSize;
        this.singleRow = singleRow;
        this.bandAll = singleRow && bandAll;
        this.density = density;
        this.namesRoom = names ? 22 * density : 0;
        this.lightGlass = lightGlass;
        this.evenPadding = evenPadding;
        this.bandVisible = bandVisible;
        band.setColor(translucent ? 0x8C121014 : (Math.round(opacity * 2.55f) << 24) | 0x141416);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(density);
        edge.setColor(lightGlass ? 0x38FFFFFF : translucent ? 0x26FFFFFF : 0x1FFFFFFF);
    }

    /** Concentric corners: the band's radius is the tiles' radius plus the band's padding around them. */
    static float radiusPx(float tileRadiusPx, float paddingPx) {
        return tileRadiusPx + paddingPx;
    }

    /**
     * The band's padding on every side for dock_even_padding: the gap between two dock tiles (a cell minus its tile, as
     * AppAdapter.tileWidthPx lays them out), capped by the room the grid leaves at its edges.
     */
    static float evenPaddingPx(int innerWidthPx, int columns, int tileWidthPx, float roomPx) {
        float gap = columns > 0 ? innerWidthPx / (float) columns - tileWidthPx : 0f;
        return Math.max(0f, Math.min(gap, roomPx));
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        if (pos >= 0 && (singleRow || pos < dockSize)) {
            outRect.top = (int) (6 * density);
            outRect.bottom = (int) (12 * density);
        }
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (!(custom==null?bandVisible:custom.visible)) return;
        boolean any = false;
        int cardWidth = 0;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            int pos = parent.getChildAdapterPosition(child);
            if (pos < 0 || (!bandAll && pos >= dockSize)) continue;
            RecyclerView.ViewHolder vh = parent.getChildViewHolder(child);
            View card = vh instanceof AppAdapter.Holder ? ((AppAdapter.Holder) vh).card : child.findViewById(R.id.card);
            if (card == null) continue;
            float l = child.getLeft() + card.getLeft();
            float t = child.getTop() + card.getTop() + child.getTranslationY();
            float r = l + card.getWidth();
            float b = t + card.getHeight();
            cardWidth = card.getWidth();
            if (!any) {
                rect.set(l, t, r, b);
                any = true;
            } else {
                rect.union(l, t, r, b);
            }
        }
        if (!any) return;
        float pad = 14 * density;
        // The band can only draw inside the grid, and the first and last dock tiles sit 12dp in from its
        // edges: a full 14dp at the sides sliced the rounded ends flat (#454). Keep the sides, with the
        // 1dp edge line, inside the grid; the top and bottom have room.
        // 22dp at the sides (the grid's 10dp padding makes room): a focused first or last tile grows about 7dp
        // outwards and still keeps about the band's top margin from its rounded end.
        float sidePad = 22 * density;
        float side = Math.max(0f, Math.min(sidePad, Math.min(rect.left, parent.getWidth() - rect.right) - density));
        float radius = AppAdapter.cornerRadius(cardWidth, density);
        if (evenPadding) {
            // The same padding on all four sides, so the band's corners are concentric with the tiles' everywhere.
            int inner = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
            // A single row's tiles can run off the screen, so only its top limits the padding; its ends are clamped below.
            float room = (singleRow ? rect.top : Math.min(rect.top, Math.min(rect.left, parent.getWidth() - rect.right))) - density;
            float even = evenPaddingPx(inner, dockSize, cardWidth, room);
            rect.inset(-even, -even);
            radius = radiusPx(radius, even);
        } else {
            // dock_even_padding off: the band as before, with its fixed 26 dp corners.
            rect.inset(singleRow ? -sidePad : -side, -pad);
            radius = 26 * density;
        }
        if (singleRow) {
            // Tiles past the screen's edge: the band stops at the grid's edge with its rounded end instead of being
            // sliced square there.
            rect.left = Math.max(rect.left, density);
            rect.right = Math.min(rect.right, parent.getWidth() - density);
        }
        rect.bottom += namesRoom;
        if(custom!=null&&!"legacy".equals(custom.mode)){drawCustom(c);return;}
        if (lightGlass && (rect.top != shaderTop || rect.bottom != shaderBottom)) {
            // The pills' glass (pill_bg: white, 18% fading to 8%), a little fainter over this much larger area.
            band.setShader(new LinearGradient(0, rect.top, 0, rect.bottom, 0x24FFFFFF, 0x0DFFFFFF,
                    Shader.TileMode.CLAMP));
            shaderTop = rect.top;
            shaderBottom = rect.bottom;
        }
        c.drawRoundRect(rect, radius, radius, band);
        c.drawRoundRect(rect, radius, radius, edge);
    }
    private void drawCustom(Canvas canvas){
        if("quickaccess".equals(custom.mode)){quickAccess.draw(canvas,rect,custom,density);return;}
        if(!paintValid||!painted.equals(rect)){
            paintValid=true;painted.set(rect);int opacity=100-custom.transparency;boolean glass="glass".equals(custom.mode);
            customFill.setShader(null);customFill.setColor(DockStyle.alpha(custom.top,opacity));
            if(!"solid".equals(custom.mode)){customFill.setColor(0xFFFFFFFF);customFill.setShader(new LinearGradient(rect.left,rect.top,rect.right,rect.bottom,glass?new int[]{DockStyle.alpha(DockStyle.mix(custom.top,0xFFFFFFFF,.24f),opacity),DockStyle.alpha(custom.top,Math.round(opacity*.85f)),DockStyle.alpha(custom.bottom,Math.round(opacity*.7f))}:new int[]{DockStyle.alpha(custom.top,opacity),DockStyle.alpha(custom.bottom,opacity)},null,Shader.TileMode.CLAMP));}
            customEdge.setStyle(Paint.Style.STROKE);customEdge.setStrokeWidth(density*.65f);
            customEdge.setShader(new LinearGradient(rect.left,rect.top,rect.right,rect.bottom,new int[]{DockStyle.alpha(custom.rimColor,Math.min(100,custom.rimOpacity*(glass?2:1))),DockStyle.alpha(custom.rimColor,custom.rimOpacity/2),DockStyle.alpha(custom.rimColor,custom.rimOpacity)},null,Shader.TileMode.CLAMP));
            glint.setShader(new LinearGradient(rect.left,rect.top,rect.right,rect.bottom,new int[]{DockStyle.alpha(0xFFFFFF,Math.round(opacity*.2f)),0x00FFFFFF,DockStyle.alpha(0xFFFFFF,Math.round(opacity*.12f)),0x00FFFFFF},new float[]{0,.35f,.58f,1},Shader.TileMode.CLAMP));
            innerEdge.setStyle(Paint.Style.STROKE);innerEdge.setStrokeWidth(density*.5f);innerEdge.setShader(new LinearGradient(0,rect.top,0,rect.bottom,DockStyle.alpha(0xFFFFFF,Math.round(opacity*.35f)),0x00FFFFFF,Shader.TileMode.CLAMP));
        }
        float radius=Math.min(custom.radius*density,Math.min(rect.width(),rect.height())/2);canvas.drawRoundRect(rect,radius,radius,customFill);
        if("glass".equals(custom.mode)&&custom.gloss){canvas.drawRoundRect(rect,radius,radius,glint);inner.set(rect);inner.inset(density,density);canvas.drawRoundRect(inner,Math.max(0,radius-density),Math.max(0,radius-density),innerEdge);}
        if(custom.rim)canvas.drawRoundRect(rect,radius,radius,customEdge);
    }

}
