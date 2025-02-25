//
//  VLSelectSongTableItemView.m
//  VoiceOnLine
//

#import "VLSelectSongTableItemView.h"
#import "VLSelectedSongListCell.h"
#import "VLSongItmModel.h"
#import "VLMacroDefine.h"
#import "VLURLPathConfig.h"
#import "VLUserCenter.h"
#import "VLToast.h"
#import "AppContext+KTV.h"
#import "AESMacro.h"
#import "NSString+Helper.h"
#import "YYModel/YYModel.h"
@import MJRefresh;

@interface VLSelectSongTableItemView ()<
UITableViewDataSource,
UITableViewDelegate
>
@property (nonatomic, strong) UITableView    *tableView;
@property (nonatomic, strong) NSMutableArray *songsMuArray;

@property (nonatomic, copy) NSString *roomNo;

@end

@implementation VLSelectSongTableItemView

- (void)setSelSongsArray:(NSArray *)selSongsArray {
    _selSongsArray = selSongsArray;
    
    [self calcSelectedStatus];
    [self.tableView reloadData];
}

- (void)dealloc {
}

- (instancetype)initWithFrame:(CGRect)frame
                    withRooNo:(NSString *)roomNo {
    if (self = [super initWithFrame:frame]) {
        self.backgroundColor = UIColorMakeWithHex(@"#152164");
        self.roomNo = roomNo;
        [self setupView];
    }
    return self;
}

#pragma mark private method
- (void)setupView {
    self.tableView = [[UITableView alloc]initWithFrame:CGRectMake(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT*0.7-20-22-15-40-40-4-20)];
    self.tableView.dataSource = self;
    self.tableView.delegate = self;
    self.tableView.separatorStyle = UITableViewCellSeparatorStyleNone;
    self.tableView.backgroundColor = UIColorMakeWithHex(@"#152164");
    [self addSubview:self.tableView];
    
    VL(weakSelf);
    self.tableView.mj_header = [MJRefreshNormalHeader headerWithRefreshingBlock:^{
        [weakSelf loadDatasWithIfRefresh:YES];
    }];
    [self.tableView.mj_header beginRefreshing];
}

-(void)loadData {
    [self.tableView.refreshControl beginRefreshing];
    [self loadDatasWithIfRefresh:YES];
}

- (void)calcSelectedStatus {
    for (VLSongItmModel *itemModel in self.songsMuArray) {
        itemModel.ifChoosed = NO;
        for (VLRoomSelSongModel *selModel in self.selSongsArray) {
            if ([itemModel.songNo isEqualToString:selModel.songNo]) {
                itemModel.ifChoosed = YES;
                break;
            }
        }
    }
}

- (void)appendDatasWithSongList:(NSArray<VLSongItmModel*>*)songList {
    [self.tableView.mj_header endRefreshing];
    if (songList.count == 0) {
        return;
    }
    NSArray *modelsArray = songList;
    [self.songsMuArray removeAllObjects];
    self.songsMuArray = modelsArray.mutableCopy;
    [self updateData];
}


#pragma mark public method
- (UIView *)listView {
    return self;
}

- (void)loadDatasWithIfRefresh:(BOOL)ifRefresh {
    [[AppContext shared].ktvAPI fetchSongListWithComplete:^(NSArray * _Nonnull songs) {
        NSMutableArray *temp = [NSMutableArray array];
        [songs enumerateObjectsUsingBlock:^(id  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
            KTVSongModel *model = obj;
            VLSongItmModel *newModel = [[VLSongItmModel alloc] init];
            newModel.singer = model.singer;
            newModel.songName = model.name;
            newModel.singer = model.singer;
            newModel.songNo = model.songCode;
            newModel.lyric = model.lyric;
            [temp addObject:newModel];
        }];
        dispatch_async(dispatch_get_main_queue(), ^{
            [self appendDatasWithSongList:temp];
        });
    }];
}

#pragma mark -- UITableViewDataSource UITableViewDelegate
- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return self.songsMuArray.count;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    VL(weakSelf);
    static NSString *reuseCell = @"reuse";
    VLSelectedSongListCell *cell = [tableView dequeueReusableCellWithIdentifier:reuseCell];
    if (cell == nil) {
        cell = [[VLSelectedSongListCell alloc]initWithStyle:UITableViewCellStyleDefault reuseIdentifier:reuseCell];
    }
    cell.songItemModel = self.songsMuArray[indexPath.row];
    cell.dianGeBtnClickBlock = ^(VLSongItmModel * _Nonnull model) {
        [weakSelf dianGeWithModel:model];
    };
    return cell;
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
    return 78;
}

- (void)dianGeWithModel:(VLSongItmModel*)model {
    if(model == nil || model.songNo == nil || model.songName == nil ) {
        [VLToast toast:KTVLocalizedString(@"ktv_chooseSong_failed")];
        return;
    }
    
    KTVChooseSongInputModel* inputModel = [KTVChooseSongInputModel new];
    inputModel.songName = model.songName;
    inputModel.songNo = model.songNo;
//    inputModel.songUrl = model.songUrl;
    inputModel.imageUrl = model.imageUrl;
    inputModel.singer = model.singer;
    [[AppContext ktvServiceImp] chooseSongWithInputModel:inputModel
                                              completion:^(NSError * error) {
        if (error != nil) {
            [self dianGeFailedWithModel:model];
            [VLToast toast:KTVLocalizedString(@"ktv_choose_fail") duration:2];
            return;
        }
        //Send notification after ordering the song
        [self dianGeSuccessWithModel:model];
    }];
}

- (void)dianGeFailedWithModel:(VLSongItmModel *)songItemModel {
    for (VLSongItmModel *model in self.songsMuArray) {
        if (songItemModel.songNo == model.songNo) {
            model.ifChoosed = NO;
        }
    }
    [self.tableView reloadData];
}


- (void)dianGeSuccessWithModel:(VLSongItmModel *)songItemModel {
    for (VLSongItmModel *model in self.songsMuArray) {
        if (songItemModel.songNo == model.songNo) {
            model.ifChoosed = YES;
        }
    }
    [self.tableView reloadData];
}

//Update the status of the songs ordered by others
- (void)setSelSongArrayWith:(NSArray *)array {
    self.selSongsArray = array;
    [self updateData];
}

-(void)updateData  {
    for (VLSongItmModel *itemModel in self.songsMuArray) {
        for (VLRoomSelSongModel *selModel in self.selSongsArray) {
            if ([itemModel.songNo isEqualToString:selModel.songNo]) {
                itemModel.ifChoosed = YES;
            }
        }
    }
    
    [self.tableView reloadData];
}

- (NSMutableArray *)songsMuArray {
    if (!_songsMuArray) {
        _songsMuArray = [NSMutableArray array];
    }
    return _songsMuArray;
}

@end
