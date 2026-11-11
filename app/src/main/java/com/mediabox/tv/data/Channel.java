package com.mediabox.tv.data;

import java.util.Objects;

/** ช่องหนึ่งรายการจาก playlist (.m3u / .m3u8) */
public final class Channel {

    private final String name;
    private final String url;
    private final String group;
    private final String logo;

    public Channel(String name, String url, String group, String logo) {
        this.name = name == null ? "" : name;
        this.url = url == null ? "" : url;
        this.group = group == null ? "" : group;
        this.logo = logo == null ? "" : logo;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getGroup() {
        return group;
    }

    public String getLogo() {
        return logo;
    }

    public boolean isPlayable() {
        return (url.startsWith("http://") || url.startsWith("https://")) && url.length() > 8;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Channel)) {
            return false;
        }
        Channel other = (Channel) o;
        return name.equals(other.name)
                && url.equals(other.url)
                && group.equals(other.group)
                && logo.equals(other.logo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url, group, logo);
    }

    @Override
    public String toString() {
        return "Channel{" + name + " | " + group + " | " + url + "}";
    }
}
