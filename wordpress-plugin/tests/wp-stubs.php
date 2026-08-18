<?php
/**
 * PixelStore Bridge testleri için asgari WordPress taklidi.
 *
 * Amaç tam bir WordPress kurulumu olmadan köprü mantığını (alan eşleme, puan aritmetiği,
 * istek listesi, jeton doğrulama, yetki) doğrulamak. Yalnızca eklentinin gerçekten çağırdığı
 * fonksiyonlar taklit edilir.
 */

define( 'ABSPATH', __DIR__ . '/' );
define( 'PSB_VERSION', '1.0.0' );
define( 'PSB_NAMESPACE', 'pixelstore/v2' );
define( 'PSB_DIR', dirname( __DIR__ ) . '/pixelstore-bridge/' );
define( 'PSB_POST_TYPE', 'game' );
define( 'PSB_REPORT_POST_TYPE', 'sl_report' );

$GLOBALS['db'] = array(
	'posts'        => array(),
	'postmeta'     => array(),
	'usermeta'     => array(),
	'commentmeta'  => array(),
	'comments'     => array(),
	'users'        => array(),
	'terms'        => array(),   // taxonomy => array(term_id => object)
	'post_terms'   => array(),   // post_id => taxonomy => array(term_id)
	'thumbnails'   => array(),
	'attachments'  => array(),
	'next_id'      => 1000,
);

function psb_next_id() {
	return ++$GLOBALS['db']['next_id'];
}

/* ---- Temel sınıflar --------------------------------------------------------------------------- */

class WP_Error {
	public $code;
	public $message;
	public $data;
	public function __construct( $code = '', $message = '', $data = array() ) {
		$this->code    = $code;
		$this->message = $message;
		$this->data    = $data;
	}
	public function get_error_message() {
		return $this->message;
	}
}

function is_wp_error( $thing ) {
	return $thing instanceof WP_Error;
}

class WP_Post {
	public $ID;
	public $post_title = '';
	public $post_name = '';
	public $post_content = '';
	public $post_excerpt = '';
	public $post_status = 'publish';
	public $post_type = 'game';
	public $post_date = '2026-01-01 10:00:00';
	public $post_modified = '2026-02-01 10:00:00';
	public function __construct( array $fields = array() ) {
		foreach ( $fields as $key => $value ) {
			$this->$key = $value;
		}
	}
}

class WP_User {
	public $ID;
	public $user_login;
	public $display_name;
	public $user_email;
	public $user_registered = '2025-03-01 12:00:00';
	public $roles = array( 'subscriber' );
	public $caps = array();
	public function __construct( $id, $login, $name, $caps = array(), $email = null ) {
		$this->ID           = $id;
		$this->user_login   = $login;
		$this->display_name = $name;
		$this->user_email   = $email ?: ( $login . '@example.test' );
		$this->caps         = $caps;
	}
}

class WP_Comment {
	public $comment_ID;
	public $comment_post_ID;
	public $comment_author;
	public $comment_author_email;
	public $comment_content;
	public $comment_type;
	public $user_id;
	public $comment_approved = 1;
	public $comment_date = '2026-03-01 09:00:00';
	public function __construct( array $fields = array() ) {
		foreach ( $fields as $key => $value ) {
			$this->$key = $value;
		}
	}
}

class WP_REST_Request {
	private $headers = array();
	private $params  = array();
	private $json    = null;
	private $files   = array();
	public $route_params = array();

	public function __construct( array $params = array(), array $headers = array(), $json = null, array $files = array() ) {
		$this->params  = $params;
		$this->headers = $headers;
		$this->json    = $json;
		$this->files   = $files;
	}
	public function get_header( $key ) {
		$key = strtolower( str_replace( '-', '_', $key ) );
		return $this->headers[ $key ] ?? null;
	}
	public function get_param( $key ) {
		return $this->params[ $key ] ?? null;
	}
	public function get_json_params() {
		return $this->json;
	}
	public function get_body_params() {
		return array();
	}
	public function get_file_params() {
		return $this->files;
	}
	public function offsetGet( $key ) {
		return $this->route_params[ $key ] ?? null;
	}
	public function __get( $key ) {
		return $this->route_params[ $key ] ?? null;
	}
}

/* ---- Yazı ve meta ----------------------------------------------------------------------------- */

function get_post( $id ) {
	return $GLOBALS['db']['posts'][ (int) $id ] ?? null;
}

function get_the_title( $post ) {
	$post = is_object( $post ) ? $post : get_post( $post );
	return $post ? $post->post_title : '';
}

function has_excerpt( $post ) {
	$post = is_object( $post ) ? $post : get_post( $post );
	return $post && '' !== trim( (string) $post->post_excerpt );
}

function get_permalink( $post ) {
	$post = is_object( $post ) ? $post : get_post( $post );
	return $post ? 'https://riaslink.fun/' . $post->post_name . '/' : '';
}

function get_the_modified_date( $format, $post ) {
	$post = is_object( $post ) ? $post : get_post( $post );
	return $post ? date( $format, strtotime( $post->post_modified ) ) : '';
}

function get_post_status( $id ) {
	$post = get_post( $id );
	return $post ? $post->post_status : false;
}

function get_post_field( $field, $id ) {
	$post = get_post( $id );
	return $post ? ( $post->$field ?? '' ) : '';
}

function get_post_meta( $post_id, $key, $single = false ) {
	return $GLOBALS['db']['postmeta'][ (int) $post_id ][ $key ] ?? '';
}

function update_post_meta( $post_id, $key, $value ) {
	$GLOBALS['db']['postmeta'][ (int) $post_id ][ $key ] = $value;
	return true;
}

function delete_post_meta( $post_id, $key ) {
	unset( $GLOBALS['db']['postmeta'][ (int) $post_id ][ $key ] );
	return true;
}

function wp_insert_post( $arr, $wp_error = false ) {
	$id   = psb_next_id();
	$post = new WP_Post(
		array(
			'ID'           => $id,
			'post_title'   => $arr['post_title'] ?? '',
			'post_name'    => sanitize_title( $arr['post_title'] ?? ( 'kayit-' . $id ) ),
			'post_content' => $arr['post_content'] ?? '',
			'post_excerpt' => $arr['post_excerpt'] ?? '',
			'post_status'  => $arr['post_status'] ?? 'publish',
			'post_type'    => $arr['post_type'] ?? 'post',
		)
	);
	$GLOBALS['db']['posts'][ $id ] = $post;
	return $id;
}

function wp_update_post( $arr, $wp_error = false ) {
	$id   = (int) $arr['ID'];
	$post = get_post( $id );
	if ( ! $post ) {
		return new WP_Error( 'missing', 'yok' );
	}
	foreach ( $arr as $key => $value ) {
		if ( 'ID' === $key ) {
			continue;
		}
		$post->$key = $value;
	}
	return $id;
}

function wp_trash_post( $id ) {
	$post = get_post( $id );
	if ( $post ) {
		$post->post_status = 'trash';
	}
	return $post;
}

function wp_delete_post( $id, $force = false ) {
	unset( $GLOBALS['db']['posts'][ (int) $id ] );
	return true;
}

function wp_count_posts( $type ) {
	$counts = array( 'publish' => 0, 'draft' => 0, 'pending' => 0, 'private' => 0, 'trash' => 0 );
	foreach ( $GLOBALS['db']['posts'] as $post ) {
		if ( $post->post_type !== $type ) {
			continue;
		}
		if ( isset( $counts[ $post->post_status ] ) ) {
			++$counts[ $post->post_status ];
		}
	}
	return (object) $counts;
}

function post_type_exists( $type ) {
	return in_array( $type, $GLOBALS['db']['registered_types'] ?? array( 'game', 'sl_report' ), true );
}

/** get_posts: testlerde gereken alt küme (name, post__in, post_type, post_status, fields). */
function get_posts( $args ) {
	$type     = $args['post_type'] ?? 'post';
	$statuses = $args['post_status'] ?? array( 'publish' );
	$statuses = is_array( $statuses ) ? $statuses : array( $statuses );
	if ( in_array( 'any', $statuses, true ) ) {
		$statuses = array( 'publish', 'draft', 'pending', 'private' );
	}

	$out = array();
	foreach ( $GLOBALS['db']['posts'] as $post ) {
		if ( $post->post_type !== $type ) {
			continue;
		}
		if ( ! in_array( $post->post_status, $statuses, true ) ) {
			continue;
		}
		if ( isset( $args['name'] ) && $post->post_name !== $args['name'] ) {
			continue;
		}
		if ( isset( $args['post__in'] ) && ! in_array( $post->ID, array_map( 'intval', $args['post__in'] ), true ) ) {
			continue;
		}
		$out[] = $post;
	}

	if ( isset( $args['post__in'] ) && ( $args['orderby'] ?? '' ) === 'post__in' ) {
		$order = array_map( 'intval', $args['post__in'] );
		usort(
			$out,
			function ( $a, $b ) use ( $order ) {
				return array_search( $a->ID, $order, true ) <=> array_search( $b->ID, $order, true );
			}
		);
	}

	if ( ( $args['fields'] ?? '' ) === 'ids' ) {
		return array_map(
			function ( $p ) {
				return $p->ID;
			},
			$out
		);
	}
	return $out;
}

class WP_Query {
	public $posts = array();
	public $max_num_pages = 1;
	public $found_posts = 0;

	public function __construct( $args ) {
		$type     = $args['post_type'] ?? 'post';
		$statuses = $args['post_status'] ?? array( 'publish' );
		$statuses = is_array( $statuses ) ? $statuses : array( $statuses );

		$matches = array();
		foreach ( $GLOBALS['db']['posts'] as $post ) {
			if ( $post->post_type !== $type || ! in_array( $post->post_status, $statuses, true ) ) {
				continue;
			}
			if ( ! empty( $args['s'] ) ) {
				$needle = mb_strtolower( $args['s'] );
				$hay    = mb_strtolower( $post->post_title . ' ' . $post->post_content );
				if ( false === mb_strpos( $hay, $needle ) ) {
					continue;
				}
			}
			if ( ! empty( $args['tax_query'] ) ) {
				$ok = true;
				foreach ( $args['tax_query'] as $key => $clause ) {
					if ( 'relation' === $key || ! is_array( $clause ) ) {
						continue;
					}
					$slugs = psb_test_term_slugs( $post->ID, $clause['taxonomy'] );
					if ( ! array_intersect( (array) $clause['terms'], $slugs ) ) {
						$ok = false;
						break;
					}
				}
				if ( ! $ok ) {
					continue;
				}
			}
			if ( ! empty( $args['meta_query'] ) ) {
				$ok = true;
				foreach ( $args['meta_query'] as $clause ) {
					if ( ! is_array( $clause ) ) {
						continue;
					}
					if ( (string) get_post_meta( $post->ID, $clause['key'], true ) !== (string) $clause['value'] ) {
						$ok = false;
						break;
					}
				}
				if ( ! $ok ) {
					continue;
				}
			}
			$matches[] = $post;
		}

		$orderby = $args['orderby'] ?? 'date';
		if ( 'meta_value_num' === $orderby && isset( $args['meta_key'] ) ) {
			$key = $args['meta_key'];
			usort(
				$matches,
				function ( $a, $b ) use ( $key ) {
					return (float) get_post_meta( $b->ID, $key, true ) <=> (float) get_post_meta( $a->ID, $key, true );
				}
			);
		} elseif ( 'title' === $orderby ) {
			usort(
				$matches,
				function ( $a, $b ) {
					return strcmp( $a->post_title, $b->post_title );
				}
			);
		} else {
			usort(
				$matches,
				function ( $a, $b ) {
					return strtotime( $b->post_date ) <=> strtotime( $a->post_date );
				}
			);
			if ( 'ASC' === ( $args['order'] ?? 'DESC' ) ) {
				$matches = array_reverse( $matches );
			}
		}

		$this->found_posts   = count( $matches );
		$per_page            = (int) ( $args['posts_per_page'] ?? 10 );
		$paged               = max( 1, (int) ( $args['paged'] ?? 1 ) );
		$this->max_num_pages = $per_page > 0 ? (int) ceil( $this->found_posts / $per_page ) : 1;
		$this->posts         = array_slice( $matches, ( $paged - 1 ) * $per_page, $per_page );
	}
}

/* ---- Taksonomi -------------------------------------------------------------------------------- */

function psb_test_register_term( $taxonomy, $name, $icon = '' ) {
	$id   = psb_next_id();
	$term = (object) array(
		'term_id'  => $id,
		'name'     => $name,
		'slug'     => sanitize_title( $name ),
		'taxonomy' => $taxonomy,
		'count'    => 0,
	);
	$GLOBALS['db']['terms'][ $taxonomy ][ $id ] = $term;
	if ( $icon ) {
		$GLOBALS['db']['termmeta'][ $id ]['sl_term_icon'] = $icon;
	}
	return $id;
}

function psb_test_term_slugs( $post_id, $taxonomy ) {
	$ids   = $GLOBALS['db']['post_terms'][ (int) $post_id ][ $taxonomy ] ?? array();
	$slugs = array();
	foreach ( $ids as $id ) {
		if ( isset( $GLOBALS['db']['terms'][ $taxonomy ][ $id ] ) ) {
			$slugs[] = $GLOBALS['db']['terms'][ $taxonomy ][ $id ]->slug;
		}
	}
	return $slugs;
}

function taxonomy_exists( $taxonomy ) {
	return in_array(
		$taxonomy,
		array( 'game_genre', 'game_platform', 'game_language', 'game_publisher', 'game_developer', 'game_features', 'game_status', 'post_tag' ),
		true
	);
}

function get_the_terms( $post_id, $taxonomy ) {
	$ids = $GLOBALS['db']['post_terms'][ (int) $post_id ][ $taxonomy ] ?? array();
	if ( empty( $ids ) ) {
		return array();
	}
	$out = array();
	foreach ( $ids as $id ) {
		if ( isset( $GLOBALS['db']['terms'][ $taxonomy ][ $id ] ) ) {
			$out[] = $GLOBALS['db']['terms'][ $taxonomy ][ $id ];
		}
	}
	return $out;
}

function get_the_tags( $post_id ) {
	return get_the_terms( $post_id, 'post_tag' );
}

function get_terms( $args ) {
	$taxonomy = $args['taxonomy'] ?? '';
	$terms    = array_values( $GLOBALS['db']['terms'][ $taxonomy ] ?? array() );
	foreach ( $terms as $term ) {
		$term->count = 0;
		foreach ( $GLOBALS['db']['post_terms'] as $post_id => $map ) {
			if ( in_array( $term->term_id, $map[ $taxonomy ] ?? array(), true ) ) {
				++$term->count;
			}
		}
	}
	if ( ! empty( $args['hide_empty'] ) ) {
		$terms = array_values(
			array_filter(
				$terms,
				function ( $t ) {
					return $t->count > 0;
				}
			)
		);
	}
	return $terms;
}

function term_exists( $name, $taxonomy ) {
	foreach ( $GLOBALS['db']['terms'][ $taxonomy ] ?? array() as $term ) {
		if ( $term->name === $name || $term->slug === sanitize_title( $name ) ) {
			return array( 'term_id' => $term->term_id );
		}
	}
	return null;
}

function wp_insert_term( $name, $taxonomy ) {
	return array( 'term_id' => psb_test_register_term( $taxonomy, $name ) );
}

function wp_set_post_terms( $post_id, $terms, $taxonomy, $append = false ) {
	$GLOBALS['db']['post_terms'][ (int) $post_id ][ $taxonomy ] = array_map( 'intval', (array) $terms );
	return true;
}

function wp_get_post_terms( $post_id, $taxonomy, $args = array() ) {
	$terms = get_the_terms( $post_id, $taxonomy );
	if ( ( $args['fields'] ?? '' ) === 'names' ) {
		return wp_list_pluck( $terms, 'name' );
	}
	if ( ( $args['fields'] ?? '' ) === 'ids' ) {
		return wp_list_pluck( $terms, 'term_id' );
	}
	return $terms;
}

function get_term_meta( $term_id, $key, $single = false ) {
	return $GLOBALS['db']['termmeta'][ (int) $term_id ][ $key ] ?? '';
}

function update_term_meta( $term_id, $key, $value ) {
	$GLOBALS['db']['termmeta'][ (int) $term_id ][ $key ] = $value;
	return true;
}

function wp_list_pluck( $list, $field ) {
	$out = array();
	foreach ( $list as $item ) {
		$out[] = is_object( $item ) ? $item->$field : $item[ $field ];
	}
	return $out;
}

/* ---- Görseller -------------------------------------------------------------------------------- */

function psb_test_register_attachment( $id, $url ) {
	$GLOBALS['db']['attachments'][ (int) $id ] = $url;
}

function get_the_post_thumbnail_url( $post_id, $size = 'thumbnail' ) {
	$id = $GLOBALS['db']['thumbnails'][ (int) $post_id ] ?? 0;
	if ( ! $id ) {
		return false;
	}
	return wp_get_attachment_image_url( $id, $size );
}

function set_post_thumbnail( $post_id, $attachment_id ) {
	$GLOBALS['db']['thumbnails'][ (int) $post_id ] = (int) $attachment_id;
	return true;
}

function wp_get_attachment_image_url( $id, $size = 'thumbnail' ) {
	$base = $GLOBALS['db']['attachments'][ (int) $id ] ?? null;
	if ( ! $base ) {
		return false;
	}
	return 'full' === $size ? $base : str_replace( '.jpg', '-' . $size . '.jpg', $base );
}

function wp_get_attachment_url( $id ) {
	return $GLOBALS['db']['attachments'][ (int) $id ] ?? false;
}

/* ---- Kullanıcılar ----------------------------------------------------------------------------- */

function psb_test_add_user( WP_User $user ) {
	$GLOBALS['db']['users'][ $user->ID ] = $user;
}

function get_user_by( $field, $value ) {
	if ( 'id' === $field ) {
		return $GLOBALS['db']['users'][ (int) $value ] ?? false;
	}
	foreach ( $GLOBALS['db']['users'] as $user ) {
		if ( 'login' === $field && $user->user_login === $value ) {
			return $user;
		}
		if ( 'email' === $field && $user->user_email === $value ) {
			return $user;
		}
	}
	return false;
}

function get_users( $args = array() ) {
	$users = array_values( $GLOBALS['db']['users'] );
	if ( ( $args['fields'] ?? '' ) === 'ID' ) {
		return wp_list_pluck( $users, 'ID' );
	}
	return $users;
}

function count_users() {
	return array( 'total_users' => count( $GLOBALS['db']['users'] ) );
}

function get_user_meta( $user_id, $key, $single = false ) {
	return $GLOBALS['db']['usermeta'][ (int) $user_id ][ $key ] ?? '';
}

function update_user_meta( $user_id, $key, $value ) {
	$GLOBALS['db']['usermeta'][ (int) $user_id ][ $key ] = $value;
	return true;
}

function delete_user_meta( $user_id, $key ) {
	unset( $GLOBALS['db']['usermeta'][ (int) $user_id ][ $key ] );
	return true;
}

function wp_set_current_user( $id ) {
	$GLOBALS['db']['current_user'] = (int) $id;
}

function user_can( $user, $cap ) {
	return in_array( $cap, $user->caps, true );
}

function wp_authenticate( $username, $password ) {
	$user = get_user_by( 'login', $username );
	if ( $user && 'gizli-parola' === $password ) {
		return $user;
	}
	return new WP_Error( 'invalid_username', 'Bilinmeyen kullanıcı adı.' );
}

function get_avatar_url( $id_or_email, $args = array() ) {
	return 'https://secure.gravatar.com/avatar/' . md5( (string) $id_or_email );
}

/* ---- Yorumlar --------------------------------------------------------------------------------- */

function wp_insert_comment( $arr ) {
	$id      = psb_next_id();
	$comment = new WP_Comment(
		array(
			'comment_ID'           => $id,
			'comment_post_ID'      => (int) $arr['comment_post_ID'],
			'comment_author'       => $arr['comment_author'] ?? '',
			'comment_author_email' => $arr['comment_author_email'] ?? '',
			'comment_content'      => $arr['comment_content'] ?? '',
			'comment_type'         => $arr['comment_type'] ?? 'comment',
			'user_id'              => (int) ( $arr['user_id'] ?? 0 ),
			'comment_approved'     => $arr['comment_approved'] ?? 1,
		)
	);
	$GLOBALS['db']['comments'][ $id ] = $comment;
	return $id;
}

function get_comment( $id ) {
	return $GLOBALS['db']['comments'][ (int) $id ] ?? null;
}

function wp_delete_comment( $id, $force = false ) {
	unset( $GLOBALS['db']['comments'][ (int) $id ] );
	return true;
}

function get_comments( $args = array() ) {
	$out = array();
	foreach ( $GLOBALS['db']['comments'] as $comment ) {
		if ( isset( $args['post_id'] ) && (int) $comment->comment_post_ID !== (int) $args['post_id'] ) {
			continue;
		}
		if ( isset( $args['user_id'] ) && (int) $comment->user_id !== (int) $args['user_id'] ) {
			continue;
		}
		if ( isset( $args['type'] ) && $comment->comment_type !== $args['type'] ) {
			continue;
		}
		$out[] = $comment;
	}
	if ( ! empty( $args['count'] ) ) {
		return count( $out );
	}
	return $out;
}

function add_comment_meta( $comment_id, $key, $value ) {
	$GLOBALS['db']['commentmeta'][ (int) $comment_id ][ $key ] = $value;
	return true;
}

function update_comment_meta( $comment_id, $key, $value ) {
	return add_comment_meta( $comment_id, $key, $value );
}

function get_comment_meta( $comment_id, $key, $single = false ) {
	return $GLOBALS['db']['commentmeta'][ (int) $comment_id ][ $key ] ?? '';
}

/* ---- Yardımcı işlevler ------------------------------------------------------------------------ */

function sanitize_title( $text ) {
	$text = mb_strtolower( (string) $text, 'UTF-8' );
	$map  = array( 'ç' => 'c', 'ğ' => 'g', 'ı' => 'i', 'i̇' => 'i', 'ö' => 'o', 'ş' => 's', 'ü' => 'u', 'â' => 'a', 'î' => 'i', 'û' => 'u' );
	$text = strtr( $text, $map );
	$text = preg_replace( '/[^a-z0-9]+/u', '-', $text );
	return trim( (string) $text, '-' );
}

function sanitize_file_name( $name ) {
	return preg_replace( '/[^A-Za-z0-9\.\-_]/', '-', (string) $name );
}

function wp_strip_all_tags( $text, $remove_breaks = false ) {
	$text = preg_replace( '@<(script|style)[^>]*?>.*?</\\1>@si', '', (string) $text );
	$text = strip_tags( $text );
	return $remove_breaks ? preg_replace( '/[\r\n\t ]+/', ' ', $text ) : $text;
}

function wp_kses_post( $text ) {
	return (string) $text;
}

function esc_url_raw( $url ) {
	return (string) $url;
}

function esc_html( $text ) {
	return htmlspecialchars( (string) $text, ENT_QUOTES, 'UTF-8' );
}

function esc_attr( $text ) {
	return esc_html( $text );
}

function wp_trim_words( $text, $count, $more = '…' ) {
	$words = preg_split( '/\s+/', trim( (string) $text ) );
	if ( count( $words ) <= $count ) {
		return implode( ' ', $words );
	}
	return implode( ' ', array_slice( $words, 0, $count ) ) . $more;
}

function mysql2date( $format, $date ) {
	return date( $format, strtotime( $date ) );
}

function get_bloginfo( $key ) {
	return 'Riaslink';
}

function rest_url( $path ) {
	return 'https://riaslink.fun/wp-json/' . ltrim( $path, '/' );
}

function rest_ensure_response( $value ) {
	return $value;
}

function do_blocks( $content ) {
	return $content;
}

function add_action() {}
function add_filter() {}
function register_rest_route() {}
function current_time( $type ) {
	return date( 'Y-m-d H:i:s' );
}
function current_user_can( $cap ) {
	return true;
}

require_once PSB_DIR . 'includes/class-psb-auth.php';
require_once PSB_DIR . 'includes/class-psb-mapper.php';
require_once PSB_DIR . 'includes/class-psb-query.php';
require_once PSB_DIR . 'includes/class-psb-social.php';
require_once PSB_DIR . 'includes/class-psb-write.php';

function psb_theme_active() {
	return true;
}
