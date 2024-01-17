# Guidelines

## Architecture

This is mainly a modified MVVM + FLUX architecture, taking good things from both.
The app is mainly composed of 3 distinct layers, namely; action, data and view.

- `View` is what user interacts with
- `Data` is what gets displayed into views
- `Action` is what happens when user interacts with view

### Data

- Data always flows uni-directionally i.e. from Data layer to View layer, while action flows in the
  opposite direction of data.  
  `Action` ---> `Data` ----> `View`  
  `Data` <--- `Action` <--- `View`
- The change in data is observable by view, hence the flow of data from data layer to view layer.
- View never changes data directly, only action can change data.

### Action

- Action manipulates data or makes changes to the system and fetches relevant data.
- Ideally only view should trigger action, unless if unavoidable

### View

- Anything that is visible to the user belongs to view category.
- View never changes data directly, it should call action which in-turn changes data.
- View subscribes to listen for changes in relevant data items and updates itself accordingly