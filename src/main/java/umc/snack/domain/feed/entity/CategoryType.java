package umc.snack.domain.feed.entity;

public enum CategoryType {
    POLITICS("100", "정치"),
    ECONOMY("101", "경제"),
    SOCIETY("102", "사회"),
    CULTURE("103", "생활/문화"),
    WORLD("104", "세계"),
    TECH("105", "IT/과학"),
    ETC("000", "기타");

    private final String sid;
    private final String categoryName;

    CategoryType(String sid, String categoryName) {
        this.sid = sid;
        this.categoryName = categoryName;
    }

    public static CategoryType fromSid(String sid) {
        for (CategoryType type : values()) {
            if (type.sid.equals(sid)) return type;
        }
        return ETC;
    }

    public String getCategoryName() {
        return this.categoryName.trim();
    }
}
