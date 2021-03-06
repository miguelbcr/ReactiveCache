/*
 * Copyright 2016 Victor Albertos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivecache2;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.rx_cache2.RxCacheException;
import java.util.ArrayList;
import java.util.List;

final class ExceptionAdapter {

  Completable completeOnRxCacheLoaderError(Throwable error) {
    if (error instanceof CompositeException) {
      for (Throwable e: ((CompositeException) error).getExceptions()) {
        if (e instanceof RxCacheException) return Completable.complete();
      }
    }

    if (error instanceof RxCacheException) return Completable.complete();

    return Completable.error(error);
  }

  <E> Observable<E> placeholderLoader() {
    return Observable.error(new PlaceHolderLoader());
  }

  <E> Single<E> stripPlaceholderLoaderException(Throwable error) {
    if (!(error instanceof CompositeException)) return Single.error(error);

    return Single.fromObservable(Observable.just(((CompositeException)error).getExceptions())
        .flatMapIterable(errors -> errors)
        .filter(e -> !(e instanceof PlaceHolderLoader))
        .toList().toObservable()
        .flatMap(curatedErrors -> {
          if (curatedErrors.size() == 1) return Observable.error(curatedErrors.get(0));
          else return Observable.error(new CompositeException(curatedErrors));
        }));
  }


  <T> Single<List<T>> emptyListIfRxCacheException(Throwable error) {
    if (error instanceof RxCacheException) return Single.just(new ArrayList<T>());
    else return Single.error(error);
  }

  static class PlaceHolderLoader extends Exception {

  }

}
