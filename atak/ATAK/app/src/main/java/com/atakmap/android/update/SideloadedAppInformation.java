
package com.atakmap.android.update;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

import com.atak.plugins.impl.AtakPluginRegistry;
import com.atakmap.coremap.filesystem.FileSystemUtils;

/**
 * Represents a side loaded application on the local device
 */
public class SideloadedAppInformation extends ProductInformation {

    private final Context _context;

    public SideloadedAppInformation(ProductRepository parent,
            Context context,
            ApplicationInfo applicationInfo) {
        //Note we use targetSDK for OS requirement, rather than min SDK, buts its all thats available

        super(parent, Platform.Android,
                ProductType.getSpecificPluginType(applicationInfo.packageName,
                        ProductType.app),
                applicationInfo.packageName,
                AppMgmtUtils.getAppNameOrPackage(context,
                        applicationInfo.packageName),
                AppMgmtUtils
                        .getAppVersionName(context,
                                applicationInfo.packageName),
                AppMgmtUtils
                        .getAppVersionCode(context,
                                applicationInfo.packageName),
                null, null,
                getString(AppMgmtUtils
                        .getAppDescription(context,
                                applicationInfo.packageName)),
                null, AppMgmtUtils.getMinSdkVersion(
                        context, applicationInfo.packageName),
                AtakPluginRegistry.getPluginApiVersion(
                        context,
                        applicationInfo.packageName, false),
                AppMgmtUtils.getAppVersionCode(context,
                        applicationInfo.packageName));
        _context = context;
    }

    @Override
    public boolean isValid() {
        return platform != null
                && productType != null
                && !FileSystemUtils.isEmpty(packageName)
                && !FileSystemUtils.isEmpty(simpleName)
                && revision >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        SideloadedAppInformation that = (SideloadedAppInformation) o;

        return _context != null ? _context.equals(that._context)
                : that._context == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_context != null ? _context.hashCode() : 0);
        return result;
    }

    @Override
    public Drawable getIcon() {
        return AppMgmtUtils.getAppDrawable(_context, packageName);
    }

}
