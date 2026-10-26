package com.ruoyi.qt.mapper;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInterceptor;
import com.ruoyi.qt.domain.QtForumPost;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 开放 API 公开文章查询的真实 SQL 验证。
 * 复用 Pages 的一次性测试库，QUANTA_PAGES_MYSQL_TEST=true 时执行（CI 已启用）。
 */
@EnabledIfEnvironmentVariable(named = "QUANTA_PAGES_MYSQL_TEST", matches = "true")
class QtPagesOpenMapperMySqlTest
{
    private static final String URL =
            "jdbc:mysql://127.0.0.1:33367/qt_pages_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static SqlSessionFactory factory;

    @BeforeAll
    static void setup() throws Exception
    {
        try (Connection c = DriverManager.getConnection(URL, "root", ""))
        {
            c.createStatement().execute("create table if not exists qt_forum_post (post_id bigint not null auto_increment, title varchar(200) not null, category varchar(20) not null, visibility varchar(20) default null, content mediumtext not null, author_id bigint not null, comment_count int not null default 0, view_count bigint not null default 0, create_time datetime default current_timestamp, update_time datetime default current_timestamp on update current_timestamp, primary key (post_id))");
        }
        Configuration config = new Configuration(new Environment("pages-open-test", new JdbcTransactionFactory(),
                new UnpooledDataSource("com.mysql.cj.jdbc.Driver", URL, "root", "")));
        config.getTypeAliasRegistry().registerAlias("QtForumPost", QtForumPost.class);
        config.addInterceptor(new PageInterceptor());
        String resource = "mapper/QtForumPostMapper.xml";
        try (InputStream stream = QtPagesOpenMapperMySqlTest.class.getClassLoader().getResourceAsStream(resource))
        {
            new XMLMapperBuilder(stream, config, resource, config.getSqlFragments()).parse();
        }
        factory = new SqlSessionFactoryBuilder().build(config);
    }

    @Test
    void publicAuthorQueryExcludesManagersAndOtherAuthors() throws Exception
    {
        reset();
        try (SqlSession session = factory.openSession(true))
        {
            List<QtForumPost> rows = session.getMapper(QtForumPostMapper.class).selectPublicPostsByAuthor(7L);
            assertEquals(List.of("第二篇旧格式", "第一篇文章"),
                    rows.stream().map(QtForumPost::getTitle).toList());
        }
    }

    @Test
    void paginationAppliesAfterVisibilityFiltering() throws Exception
    {
        reset();
        try (SqlSession session = factory.openSession(true))
        {
            PageHelper.startPage(1, 1);
            List<QtForumPost> rows = session.getMapper(QtForumPostMapper.class).selectPublicPostsByAuthor(7L);
            assertEquals(2, ((Page<?>) rows).getTotal());
            assertEquals(1, rows.size());
            assertEquals("第二篇旧格式", rows.get(0).getTitle());
        }
        finally
        {
            PageHelper.clearPage();
        }
    }

    private static void reset() throws Exception
    {
        try (Connection c = DriverManager.getConnection(URL, "root", ""))
        {
            c.createStatement().execute("delete from qt_forum_post");
            insert(c, 7L, "MEMBERS", "第一篇文章", "2026-10-01 10:00:00");
            insert(c, 7L, null, "第二篇旧格式", "2026-10-02 10:00:00");
            insert(c, 7L, "MANAGERS", "经理可见", "2026-10-03 10:00:00");
            insert(c, 8L, "MEMBERS", "别人的文章", "2026-10-04 10:00:00");
        }
    }

    private static void insert(Connection c, long authorId, String visibility, String title, String time)
    throws SQLException
    {
        try (PreparedStatement ps = c.prepareStatement(
                "insert into qt_forum_post(title,category,visibility,content,author_id,create_time,update_time)"
                        + " values (?,?,?,?,?,?,?)"))
        {
            ps.setString(1, title);
            ps.setString(2, "TECH");
            ps.setString(3, visibility);
            ps.setString(4, "正文");
            ps.setLong(5, authorId);
            ps.setString(6, time);
            ps.setString(7, time);
            ps.executeUpdate();
        }
    }
}
