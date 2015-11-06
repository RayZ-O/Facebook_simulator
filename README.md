# Facebook API Simulator
Implementation of a subset of RESTful Facebook Graph API in Scala, Akka and spray.

## Root nodes:
This is a subset of root nodes of Graph API. The main difference between a root node and a non-root node is that root nodes can be queried directly, while non-root nodes can be queried via root nodes or edges.
* Album
* Comment
* FriendList
* Group
* Page
* Photo
* Post
* User

Full list of Facebook Graph API https://developers.facebook.com/docs/graph-api/reference/

## API reference:
This implementation only covers the frequently used fields in each node

### Album
A photo album
#### Reading
GET  */{album-id}*

**Fields**

|Name|Type|
|----|----|
|id|string|
|can_upload|boolean|
|count|int|
|cover_photo|string|
|created_time|datetime|
|description|string|
|from|User|
|link|string|
|name|string|
|place|Page|
|privacy|string|
|type|enum{app, cover, profile, mobile, wall, normal, album}|
|updated_time|datetime|

#### Publishing
POST */{user-id}/albums, /{page-id}/albums* or */{group-id}/albums*

**Parameters**

|Name|Type|Description|
|----|----|-----------|
|id|numeric string|The Object ID that we're creating an album for|
|name|string|The title of the album|
|privacy|Privacy Parameter|The privacy of the album|
|place|place tag|The ID of a location page to tag the album with|
|message|string|The album's description|

**Return Type**
```
Struct {
  id: numeric string,
}
```

#### Deleting
You can't delete albums using the Graph API.

#### Updating
You can't update albums using the Graph API.

#### Edges

|Property Name|Description|
|-------------|-----------|
|*/picture*|The cover photo of this album.|
|*/photos*|Photos contained in this album.|
|*/sharedposts*|Stream posts that are shares of this album.|
|*/likes*|People who like this.|
|*/comments*|Comments made on this.|

### Comment
A comment can be made on various types of content on Facebook. Most Graph API nodes have a */comments* edge that lists all the comments on that object. The */{comment-id}* node returns a single comment.

#### Reading
GET  */{comment-id}*

**Fields**

|Property Name|Type|
|-------------|----|
|id|string|
|attachment|StoryAttachment|
|can_comment|bool|
|can_remove|bool|
|can_hide|boolean|
|can_like|boolean|
|comment_count|int32|
|created_time|datetime|
|from|User|
|like_count|int32|
|message|string|
|object|Object(Parent object of comment)|
|parent|Comment|
|user_likes|bool|

#### Publishing
Using the */comments* edge when it is present on a node.
POST */{object-id}/comments*

**Parameters**
One of attachment_url, attachment_id, message or source must be provided when publishing.

|Name|Description|Type|
|----|-----------|----|
|message|The comment text.|string|
|attachment_id|ID of a unpublished photo uploaded to include as a photo comment.|string|
|attachment_url|The URL of an image to include as a photo comment.|string|
|source|A photo, encoded as form data, to use as a photo comment.|multipart/form-data|

**Response**
If successful:

|Name|Description|Type|
|----|-----------|----|
|id|The newly created comment ID|string|

#### Deleting
DELETE *{comment-id}*

**Response**
If successful:
```
{
  "success": true
}
```
Otherwise a relevant error message will be returned.

#### Updating
POST */{comment-id}*

**Fields**

|Property Name|Type|
|-------------|----|
|message|string|
|is_hidden|boolean|

**Response**
If successful:
```
{
  "success": true
}
```

#### Edges

|Property Name|Description|
|-------------|-----------|
|*/likes*|People who like this comment.|
|*/comments*|Comments that reply to this comment.|
